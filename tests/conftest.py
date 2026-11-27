import os

os.environ["DATABASE_URL"] = "sqlite://"      # DB trong RAM cho test
os.environ["ADMIN_TOKEN"] = "test-admin"

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture()
def client():
    with TestClient(app) as c:   # chạy lifespan: tạo bảng + nạp nội dung
        yield c


@pytest.fixture()
def auth(client):
    r = client.post("/users", json={"display_name": "Lan"})
    assert r.status_code == 201
    return {"Authorization": f"Bearer {r.json()['token']}"}
