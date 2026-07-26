from __future__ import annotations

from datetime import datetime, timezone

from fastapi import FastAPI
from pydantic import BaseModel, Field

from .model import CategoryModel

app = FastAPI(
    title="SpendWise ML Service",
    version="1.0.0",
    description="Transaction category prediction and feedback learning service.",
)
model = CategoryModel()


class CategoryRequest(BaseModel):
    user_id: int
    merchant: str = Field(min_length=1, max_length=160)
    description: str = Field(default="", max_length=500)
    amount: float = Field(gt=0)
    available_categories: list[str] = Field(default_factory=list)


class CategoryResponse(BaseModel):
    category: str
    confidence: float
    model: str


class FeedbackRequest(BaseModel):
    user_id: int
    merchant: str = Field(min_length=1, max_length=160)
    description: str = Field(default="", max_length=500)
    corrected_category: str = Field(min_length=1, max_length=80)


class BudgetRiskRequest(BaseModel):
    spent: float = Field(ge=0)
    limit: float = Field(gt=0)
    day_of_month: int = Field(ge=1, le=31)
    days_in_month: int = Field(ge=28, le=31)


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "service": "spendwise-ml-service",
        "time": datetime.now(timezone.utc).isoformat(),
    }


@app.post("/predict-category", response_model=CategoryResponse)
def predict_category(request: CategoryRequest) -> CategoryResponse:
    prediction = model.predict(
        user_id=request.user_id,
        merchant=request.merchant,
        description=request.description,
        available_categories=request.available_categories,
    )
    return CategoryResponse(
        category=prediction.category,
        confidence=prediction.confidence,
        model=prediction.model,
    )


@app.post("/feedback")
def feedback(request: FeedbackRequest) -> dict:
    model.add_feedback(
        user_id=request.user_id,
        merchant=request.merchant,
        description=request.description,
        corrected_category=request.corrected_category,
    )
    return {"saved": True, "message": "Correction saved and model refreshed."}


@app.post("/predict-budget-risk")
def predict_budget_risk(request: BudgetRiskRequest) -> dict:
    projected = request.spent / request.day_of_month * request.days_in_month
    projected_percent = projected / request.limit * 100
    if projected_percent >= 120:
        risk = "HIGH"
    elif projected_percent >= 100:
        risk = "MEDIUM"
    else:
        risk = "LOW"
    return {
        "projected_spend": round(projected, 2),
        "projected_percent": round(projected_percent, 1),
        "risk": risk,
    }
