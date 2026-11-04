-- 依据用户提供的 Container / PQueue 片段重建。
-- 原文部分符号在传输中丢失，以下为按标准 LSL 习惯补回的可解析版本。
Container(E, C): trait
  includes InsertGenerated, Integer
  introduces
    isEmpty: C → Bool
    count: E, C → Int
    __ ∈ __: E, C → Bool
    head: C → E
    tail: C → C
  asserts
    C partitioned by isEmpty, head, tail
    e, e1: E, c: C
    isEmpty(empty);
    ~isEmpty(insert(e, c));
    count(e, empty) == 0;
    count(e, insert(e1, c)) ==
      count(e, c) + (if e = e1 then 1 else 0);
    (e ∈ c) == count(e, c) > 0;
    count(e, insert(head(c), tail(c))) == count(e, c)
  implies
    c: C
    ~isEmpty(c) ⇒ count(head(c), c) > 0;
  converts isEmpty, count, ∈

PQueue(E, Q): trait
  assumes TotalOrder(E)
  includes Container(Q for C)
  asserts e, e1: E, q: Q
    head(insert(e, q)) ==
      if isEmpty(q) ∨ e ≤ head(q) then e else head(q);
    tail(insert(e, q)) ==
      if isEmpty(q) ∨ e ≤ head(q) then q else insert(e, tail(q))
  implies
    q: Q, e: E
    e ∈ q ⇒ e ≤ head(q);
  converts head, tail, isEmpty, count
  exempting head(empty), tail(empty)
