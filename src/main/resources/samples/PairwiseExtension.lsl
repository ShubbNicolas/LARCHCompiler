-- 依据用户提供的 PairwiseExtension / PairwiseSum 片段重建，
-- 原文部分运算符在传输中丢失，此处按 trait 自身的 ⊕ / ⊗ 补回。
PairwiseExtension(⊕, ⊗, E, C): trait
  assumes Container(E, C)
  introduces
    __ ⊕ __: E, E → E
    __ ⊗ __: C, C → C
  asserts e1, e2: E, c1, c2: C
    empty ⊗ empty == empty;
    (~isEmpty(c1) ∧ ~isEmpty(c2)) ⇒
      c1 ⊗ c2 == insert(head(c1) ⊕ head(c2),
                        tail(c1) ⊗ tail(c2));
  implies
  converts ⊗
  exempting e: E, c: C
    empty ⊗ insert(e, c),
    insert(e, c) ⊗ empty

PairwiseSum(C): trait
  assumes Container(Int, C)
  includes Integer, PairwiseExtension(+, *, Int, C)
  implies Associative(⊗, C),
    Commutative(⊗ for ⊕, C for T, C for Range)
