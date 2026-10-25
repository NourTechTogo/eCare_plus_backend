"""Simplify existing fictitious local accounts through authenticated APIs.

Run after seeding the local 8081 demonstration. Keeps account IDs and data.
Passwords remain in the ignored .maintenance/runtime-secrets.json file.
No SQL access, authentication bypass or production password-policy change.
"""

import json
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[2]


def main():
    config = json.loads((ROOT / "config/development-accounts.json").read_text(encoding="utf-8-sig"))
    secrets = json.loads((ROOT / ".maintenance/runtime-secrets.json").read_text(encoding="utf-8-sig"))
    api = config["api"].rstrip("/")
    address = urlsplit(api)
    if (address.scheme, address.hostname, address.port, address.path) != (
        "http", "127.0.0.1", 8081, "/api/v1"
    ):
        raise RuntimeError("Only the isolated local demo on 127.0.0.1:8081 is supported")
    password = secrets["APP_DEMO_LOCAL_PASSWORD"]

    def call(method, path, body=None, token=None):
        headers = {"Accept": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        data = None
        if body is not None:
            data = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
        request = Request(api + path, data=data, headers=headers, method=method)
        with urlopen(request, timeout=20) as response:
            payload = response.read()
            return json.loads(payload) if payload else None

    def login(email, candidate):
        return call("POST", "/auth/connexion", {"identifiant": email, "motDePasse": candidate})

    # Authenticate every account before changing any of them.
    authenticated = []
    for account in config["accounts"]:
        session = None
        for email in (account["email"], account["previousEmail"]):
            for candidate in dict.fromkeys((password, secrets["APP_DEMO_PASSWORD"])):
                try:
                    session = login(email, candidate)
                    authenticated.append((account, session, candidate))
                    break
                except HTTPError as error:
                    if error.code != 401:
                        raise RuntimeError("Demo authentication failed with HTTP " + str(error.code)) from None
            if session:
                break
        if not session:
            raise RuntimeError("Seed the demo account first: " + account["previousEmail"])

    proof = []
    for account, session, previous_password in authenticated:
        token = session["jeton"]
        before = call("GET", "/compte", token=token)
        if before["email"] != account["email"]:
            call("PATCH", "/compte", {"email": account["email"]}, token)
        if previous_password != password:
            call("POST", "/compte/mot-de-passe", {
                "motDePasseActuel": previous_password,
                "nouveauMotDePasse": password,
            }, token)
        verified = login(account["email"], password)
        after = call("GET", "/compte", token=verified["jeton"])
        if before["id"] != after["id"] or after["email"] != account["email"]:
            raise RuntimeError("Account identity verification failed")
        proof.append({"role": account["role"], "email": after["email"], "id": after["id"], "login": "PASS", "identityPreserved": True})

    target = ROOT / ".maintenance/integration-clients/development-accounts-proof.json"
    target.write_text(json.dumps(proof, ensure_ascii=False, indent=2), encoding="utf-8")
    fixture = ROOT / ".maintenance/integration-clients/fixture.json"
    if fixture.exists():
        values = json.loads(fixture.read_text(encoding="utf-8-sig"))
        values["patientEmail"] = config["accounts"][0]["email"]
        fixture.write_text(json.dumps(values, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(proof, ensure_ascii=False))


if __name__ == "__main__":
    main()
