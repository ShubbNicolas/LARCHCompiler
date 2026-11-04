Bag2(E): trait
  assumes TotalOrder(E)
  includes Bag0, Integer
  introduces
    rangeCount: E, E, B → Int
  asserts e1, e2, e3: E, b: B
    rangeCount(e1, e2, { }) == 0;
    rangeCount(e1, e2, insert(e3, b)) ==
      rangeCount(e1, e2, b)
      + (if e1 ≤ e3 ∧ e3 ≤ e2 then 1 else 0)
  implies e1, e2, e3: E, b: B
    e1 ≤ e2 ⇒
      rangeCount(e3, e1, b) ≤ rangeCount(e3, e2, b)
