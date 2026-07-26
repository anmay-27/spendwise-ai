from tempfile import TemporaryDirectory
from pathlib import Path

from app.model import CategoryModel


def test_prediction_and_feedback():
    with TemporaryDirectory() as directory:
        model = CategoryModel(str(Path(directory) / "feedback.jsonl"))
        prediction = model.predict(1, "PVR Cinemas", "movie ticket", ["Entertainment", "Other"])
        assert prediction.category == "Entertainment"

        model.add_feedback(1, "Amazon", "gift for friend", "Gifts")
        personalized = model.predict(1, "Amazon", "another purchase", ["Shopping", "Gifts", "Other"])
        assert personalized.category == "Gifts"
