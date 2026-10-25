# Local development only; requires the prepared private .maintenance configuration.
$workspaceRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$stateFile=Join-Path $workspaceRoot '.maintenance/integration-clients/servers.json'
$state=Get-Content $stateFile -Raw | ConvertFrom-Json
$current=Get-Process -Id $state.api.pid -ErrorAction SilentlyContinue
if($current){
 if($current.ProcessName -ne 'java'){throw 'Le processus enregistré ne correspond pas à Java.'}
 $listeners=Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue
 if(@($listeners | Where-Object OwningProcess -ne $current.Id).Count){throw 'Le port 8081 appartient à un autre processus.'}
 Stop-Process -Id $current.Id
 Wait-Process -Id $current.Id -Timeout 20 -ErrorAction SilentlyContinue
}
$secrets=Get-Content (Join-Path $workspaceRoot '.maintenance/runtime-secrets.json') -Raw | ConvertFrom-Json
$runtimeFolder=Join-Path $workspaceRoot '.maintenance/integration-clients'
$databaseFolder=Join-Path $runtimeFolder 'db'
New-Item -ItemType Directory -Path $databaseFolder -Force | Out-Null
$databasePath=(Join-Path $databaseFolder 'ecare_local').Replace('\','/')
$settings=@{
 SERVER_PORT='8081';SPRING_PROFILES_ACTIVE='demo';
 SPRING_DATASOURCE_URL="jdbc:h2:file:$databasePath;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_ON_EXIT=FALSE";
 SPRING_DATASOURCE_USERNAME='sa';SPRING_DATASOURCE_PASSWORD='';
 SPRING_JPA_HIBERNATE_DDL_AUTO='update';SPRING_FLYWAY_ENABLED='false';SPRING_DOCKER_COMPOSE_ENABLED='false';
 APP_DEMO_PASSWORD=$secrets.APP_DEMO_PASSWORD;APP_DOCUMENT_REPERTOIRE=(Join-Path $runtimeFolder 'private');
 APP_JWT_SECRET=$secrets.APP_JWT_SECRET;
 APP_LIVEKIT_URL=$secrets.APP_LIVEKIT_URL;APP_LIVEKIT_API_KEY=$secrets.APP_LIVEKIT_API_KEY;APP_LIVEKIT_API_SECRET=$secrets.APP_LIVEKIT_API_SECRET;
 APP_LIVEKIT_REVOCATION_ACTIVE= $(if($secrets.APP_LIVEKIT_REVOCATION_ACTIVE){'true'}else{'false'})
}
if(-not $settings.APP_JWT_SECRET){
 $settings.APP_JWT_SECRET=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
 $secrets | Add-Member -NotePropertyName APP_JWT_SECRET -NotePropertyValue $settings.APP_JWT_SECRET -Force
 $secrets | ConvertTo-Json -Depth 10 | Set-Content (Join-Path $workspaceRoot '.maintenance/runtime-secrets.json')
}
$previous=@{}
foreach($key in $settings.Keys){$previous[$key]=[Environment]::GetEnvironmentVariable($key,'Process');[Environment]::SetEnvironmentVariable($key,$settings[$key],'Process')}
try{
 $classpath=([IO.File]::ReadAllText((Join-Path $workspaceRoot '.maintenance/runtime-classpath.txt')).Trim().Split(';') |
   Where-Object { $_.Replace('\','/') -notmatch '/target/test-classes/?$' }) -join ';'
 $apiRoot=Join-Path $workspaceRoot 'eCare_plus_backend-main/backend_ecare_plus'
 $child=Start-Process -FilePath (Get-Command java).Source -ArgumentList @('-cp',('"'+$classpath+'"'),'com.nourtech.ecareplus.EcarePlusApplication') -WorkingDirectory $apiRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $workspaceRoot '.maintenance/interface-restoration/api-persistent.stdout.log') -RedirectStandardError (Join-Path $workspaceRoot '.maintenance/interface-restoration/api-persistent.stderr.log')
 $state.api.pid=$child.Id;$state | ConvertTo-Json -Depth 4 | Set-Content $stateFile
 Write-Output ('API persistante locale démarrée sur 8081, PID '+$child.Id)
}finally{foreach($key in $previous.Keys){[Environment]::SetEnvironmentVariable($key,$previous[$key],'Process')}}
