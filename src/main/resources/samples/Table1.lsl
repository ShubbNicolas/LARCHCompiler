Table1: trait
introduces
  new: → Tab
  add: Tab, Ind, Val → Tab
  __ ∈ __: Ind, Tab → Bool
  lookup: Tab, Ind → Val
  isEmpty: Tab → Bool
  size: Tab → Int
  0, 1: → Int
  __ + __: Int, Int → Int
asserts i, i1: Ind, val: Val, t: Tab
  (i ∉ new);
  i ∉ add(t, i1, val) == i ≠ i1 ∧ i ∉ t;
  lookup(add(t, i, val), i1) ==
    if i = i1 then val else lookup(t, i1);
  size(new) == 0;
  size(add(t, i, val)) ==
    if i ∈ t then size(t) else size(t) + 1;
  isEmpty(t) == size(t) = 0
