import pytest


async def login(client, email: str, password: str) -> str:
    response = await client.post("/api/auth/login", data={"username": email, "password": password})
    assert response.status_code == 200, response.text
    return response.json()["access_token"]


@pytest.mark.asyncio
async def test_oauth_login_and_authenticated_board(client):
    token = await login(client, "user@example.com", "Member-password-123!")
    headers = {"Authorization": f"Bearer {token}"}
    assert (await client.get("/api/auth/me", headers=headers)).json()["role"] == "user"
    assert (await client.get("/api/statuses", headers=headers)).status_code == 200
    assert (await client.get("/api/tasks", headers=headers)).json()["totalElements"] == 0


@pytest.mark.asyncio
async def test_invalid_password_returns_login_error(client):
    response = await client.post(
        "/api/auth/login", data={"username": "admin@example.com", "password": "wrong-password"}
    )
    assert response.status_code == 401
    assert response.json()["message"] == "Неверный email или пароль"


@pytest.mark.asyncio
async def test_role_protection_and_request_id(client):
    response = await client.get("/api/tasks")
    assert response.status_code == 401
    assert response.headers["x-request-id"]
    member = {"Authorization": f"Bearer {await login(client, 'user@example.com', 'Member-password-123!')}"}
    response = await client.delete(
        "/api/tasks/00000000-0000-0000-0000-000000000001?version=0", headers=member
    )
    assert response.status_code == 403


@pytest.mark.asyncio
async def test_task_crud_move_conflict_and_admin_delete(client):
    admin = {"Authorization": f"Bearer {await login(client, 'admin@example.com', 'Admin-password-123!')}"}
    created = await client.post("/api/tasks", headers=admin, json={"title": "Ship board", "priority": "HIGH"})
    assert created.status_code == 201, created.text
    task = created.json()
    moved = await client.patch(
        f"/api/tasks/{task['id']}/status", headers=admin, json={"status": "IN_PROGRESS", "version": 1}
    )
    assert moved.status_code == 200, moved.text
    assert moved.json()["version"] == 2
    conflict = await client.patch(
        f"/api/tasks/{task['id']}/status", headers=admin, json={"status": "DONE", "version": 1}
    )
    assert conflict.status_code == 409
    deleted = await client.delete(f"/api/tasks/{task['id']}?version=2", headers=admin)
    assert deleted.status_code == 204


@pytest.mark.asyncio
async def test_invalid_payload_has_consistent_validation(client):
    admin = {"Authorization": f"Bearer {await login(client, 'admin@example.com', 'Admin-password-123!')}"}
    response = await client.post("/api/tasks", headers=admin, json={"title": "   ", "unexpected": True})
    assert response.status_code == 422
    body = response.json()
    assert body["message"] == "Проверьте введённые данные"
    assert body["requestId"]
    assert body["fields"]
