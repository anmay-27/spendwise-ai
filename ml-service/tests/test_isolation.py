import os
os.environ.setdefault('ML_SERVICE_KEY', 'test-service-key-that-is-longer-than-32-characters')
from app.model import CategoryModel

def test_corrections_are_private_and_persistent(tmp_path):
    path = str(tmp_path / 'feedback.jsonl')
    model = CategoryModel(path)
    original = model.predict(2, 'Amazon', 'order', ['Shopping', 'Gifts', 'Other'])
    model.add_feedback(1, 'Amazon', 'order', 'Gifts')
    assert model.predict(1, 'Amazon', 'order', ['Shopping', 'Gifts']).category == 'Gifts'
    assert model.predict(2, 'Amazon', 'order', ['Shopping', 'Gifts', 'Other']) == original
    restarted = CategoryModel(path)
    assert restarted.predict(1, 'Amazon', 'order', ['Shopping', 'Gifts']).category == 'Gifts'

def test_feedback_requires_service_auth(monkeypatch, tmp_path):
    monkeypatch.setenv('FEEDBACK_FILE', str(tmp_path / 'feedback.jsonl'))
    from fastapi.testclient import TestClient
    from app.main import app
    with TestClient(app) as client:
        payload = {'user_id': 1, 'merchant': 'Amazon', 'corrected_category': 'Gifts'}
        assert client.post('/feedback', json=payload).status_code == 401
        assert client.post('/feedback', json=payload, headers={'X-Service-Key': os.environ['ML_SERVICE_KEY']}).status_code == 200

def test_confidence_is_bounded(tmp_path):
    model = CategoryModel(str(tmp_path / 'feedback.jsonl'))
    result = model.predict(1, 'Unknown 941', 'misc', ['Other'])
    assert result.category == 'Other'
    assert 0 <= result.confidence <= 1
