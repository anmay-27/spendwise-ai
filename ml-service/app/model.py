from __future__ import annotations

import json
import os
import threading
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline


SEED_EXAMPLES: list[tuple[str, str]] = [
    ("swiggy dinner food delivery restaurant", "Food"),
    ("zomato lunch restaurant order", "Food"),
    ("pizza hut pizza dinner takeaway", "Food"),
    ("dominos pizza takeaway", "Food"),
    ("kfc fried chicken restaurant", "Food"),
    ("mcdonalds burger restaurant", "Food"),
    ("d mart groceries supermarket vegetables", "Food"),
    ("starbucks coffee cafe", "Food"),
    ("bigbasket grocery delivery", "Food"),
    ("pvr cinemas movie ticket", "Entertainment"),
    ("netflix monthly subscription streaming", "Entertainment"),
    ("spotify music subscription", "Entertainment"),
    ("bookmyshow concert ticket", "Entertainment"),
    ("steam video game purchase", "Entertainment"),
    ("gaming arcade bowling", "Entertainment"),
    ("uber office ride cab", "Travel"),
    ("ola airport taxi", "Travel"),
    ("indian railways train ticket", "Travel"),
    ("indigo flight ticket", "Travel"),
    ("metro card recharge", "Travel"),
    ("petrol fuel station", "Travel"),
    ("myntra clothes fashion", "Shopping"),
    ("amazon online order", "Shopping"),
    ("flipkart electronics purchase", "Shopping"),
    ("mall clothing shoes", "Shopping"),
    ("zara apparel", "Shopping"),
    ("ikea furniture", "Shopping"),
    ("airtel mobile recharge", "Bills"),
    ("jio broadband internet bill", "Bills"),
    ("electricity bill payment", "Bills"),
    ("house rent monthly", "Bills"),
    ("water bill", "Bills"),
    ("gas cylinder bill", "Bills"),
    ("apollo pharmacy medicines", "Healthcare"),
    ("hospital doctor consultation", "Healthcare"),
    ("diagnostic blood test", "Healthcare"),
    ("dentist appointment", "Healthcare"),
    ("health insurance premium", "Healthcare"),
    ("udemy online course", "Education"),
    ("college semester fees", "Education"),
    ("book store textbooks", "Education"),
    ("coursera certification", "Education"),
    ("coaching tuition fees", "Education"),
    ("zerodha mutual fund sip", "Investment"),
    ("groww stock investment", "Investment"),
    ("fixed deposit savings", "Investment"),
    ("nps retirement contribution", "Investment"),
    ("gold investment", "Investment"),
    ("birthday present gift", "Gifts"),
    ("flower bouquet anniversary gift", "Gifts"),
    ("gift card", "Gifts"),
    ("money sent to parents", "Family"),
    ("family household support", "Family"),
    ("school fee sibling", "Family"),
    ("unknown merchant miscellaneous payment", "Other"),
    ("cash withdrawal", "Other"),
    ("local store general expense", "Other"),
]


@dataclass(frozen=True)
class Prediction:
    category: str
    confidence: float
    model: str


class CategoryModel:
    def __init__(self, feedback_file: str | None = None) -> None:
        self.feedback_path = Path(
            feedback_file or os.getenv("FEEDBACK_FILE", "data/feedback.jsonl")
        )
        self.feedback_path.parent.mkdir(parents=True, exist_ok=True)
        self.lock = threading.Lock()
        self.feedback: list[dict] = self._load_feedback()
        self.pipeline: Pipeline = self._train()

    def _load_feedback(self) -> list[dict]:
        if not self.feedback_path.exists():
            return []
        records: list[dict] = []
        for line in self.feedback_path.read_text(encoding="utf-8").splitlines():
            try:
                records.append(json.loads(line))
            except json.JSONDecodeError:
                continue
        return records

    def _training_rows(self) -> Iterable[tuple[str, str]]:
        yield from SEED_EXAMPLES
        for item in self.feedback:
            text = self._text(item.get("merchant", ""), item.get("description", ""))
            category = str(item.get("corrected_category", "Other"))
            if text.strip() and category.strip():
                yield text, category

    def _train(self) -> Pipeline:
        rows = list(self._training_rows())
        texts = [text for text, _ in rows]
        labels = [label for _, label in rows]
        pipeline = Pipeline([
            ("tfidf", TfidfVectorizer(ngram_range=(1, 2), lowercase=True, min_df=1)),
            ("classifier", LogisticRegression(max_iter=1200, random_state=42)),
        ])
        pipeline.fit(texts, labels)
        return pipeline

    def predict(
        self,
        user_id: int,
        merchant: str,
        description: str,
        available_categories: list[str] | None = None,
    ) -> Prediction:
        normalized_merchant = merchant.strip().lower()

        # The newest explicit choice by this user has the highest priority.
        for item in reversed(self.feedback):
            if (
                int(item.get("user_id", -1)) == user_id
                and str(item.get("merchant", "")).strip().lower() == normalized_merchant
            ):
                category = str(item.get("corrected_category", "Other"))
                if not available_categories or category in available_categories:
                    return Prediction(category, 0.99, "personal-merchant-memory")

        text = self._text(merchant, description)
        keyword_prediction = self._keyword_prediction(text)
        if keyword_prediction is not None:
            if not available_categories or keyword_prediction in available_categories:
                return Prediction(keyword_prediction, 0.94, "hybrid-keyword-model")

        probabilities = self.pipeline.predict_proba([text])[0]
        classes = self.pipeline.classes_
        best_index = int(probabilities.argmax())
        category = str(classes[best_index])
        confidence = float(probabilities[best_index])

        if available_categories and category not in available_categories:
            category = "Other" if "Other" in available_categories else available_categories[0]
            confidence = min(confidence, 0.50)

        return Prediction(category, round(confidence, 4), "tfidf-logistic-regression")

    def add_feedback(
        self,
        user_id: int,
        merchant: str,
        description: str,
        corrected_category: str,
    ) -> None:
        record = {
            "user_id": user_id,
            "merchant": merchant.strip(),
            "description": description.strip(),
            "corrected_category": corrected_category.strip(),
        }
        with self.lock:
            with self.feedback_path.open("a", encoding="utf-8") as file:
                file.write(json.dumps(record, ensure_ascii=False) + "\n")
            self.feedback.append(record)
            self.pipeline = self._train()

    @staticmethod
    def _keyword_prediction(text: str) -> str | None:
        keyword_groups = {
            "Food": (
                "swiggy", "zomato", "restaurant", "cafe", "grocery", "pizza",
                "pizza hut", "dominos", "burger", "kfc", "mcdonald", "subway",
                "starbucks", "bakery", "food", "dinner", "lunch",
            ),
            "Entertainment": (
                "pvr", "netflix", "spotify", "movie", "concert", "game",
                "bookmyshow", "cinema",
            ),
            "Travel": (
                "uber", "ola", "railway", "train", "flight", "metro", "petrol",
                "fuel", "taxi", "bus",
            ),
            "Shopping": (
                "myntra", "amazon", "flipkart", "mall", "clothes", "fashion",
                "electronics", "shoes",
            ),
            "Bills": (
                "airtel", "jio", "electricity", "recharge", "internet", "rent",
                "water bill", "gas bill", "broadband",
            ),
            "Healthcare": (
                "pharmacy", "hospital", "doctor", "medicine", "diagnostic", "dentist",
            ),
            "Education": (
                "udemy", "coursera", "college", "course", "tuition", "textbook",
            ),
            "Investment": (
                "zerodha", "groww", "mutual fund", "sip", "stock", "fixed deposit", "nps",
            ),
            "Gifts": ("gift", "birthday present", "bouquet"),
            "Family": ("parents", "family support", "sibling"),
        }

        lowered = text.lower()
        for category, keywords in keyword_groups.items():
            if any(keyword in lowered for keyword in keywords):
                return category
        return None

    @staticmethod
    def _text(merchant: str, description: str) -> str:
        return f"{merchant.strip()} {description.strip()}".strip()
