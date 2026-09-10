import { useEffect, useState } from "react";
import { request } from "../api";
export function useData(path) {
  const [data, setData] = useState(null),
    [error, setError] = useState("");
  const load = () =>
    request(path)
      .then((value) => {
        setData(value);
        setError("");
      })
      .catch((e) => setError(e.message));
  useEffect(() => {
    let active = true;
    request(path)
      .then((v) => active && setData(v))
      .catch((e) => active && setError(e.message));
    return () => {
      active = false;
    };
  }, [path]);
  return { data, error, load };
}
