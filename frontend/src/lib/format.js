export const money = (value = 0) =>
  "₹" + Number(value).toLocaleString("en-IN", { maximumFractionDigits: 2 });
export const date = (value) =>
  new Date(value).toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
