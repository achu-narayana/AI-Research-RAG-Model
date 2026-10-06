from vector_store import select_representative_chunks


def test_small_paper_uses_every_chunk():
    chunks = [f"c{i}" for i in range(5)]

    assert select_representative_chunks(chunks, 24, 4) == chunks


def test_large_paper_covers_beginning_middle_and_end():
    chunks = [f"c{i}" for i in range(42)]

    selected = select_representative_chunks(chunks, 24, 4)

    assert len(selected) == 24
    assert selected[:4] == ["c0", "c1", "c2", "c3"]
    assert selected[-1] == "c41"
    # Spread over the second half too, not just the first 24 chunks.
    assert sum(int(c[1:]) >= 21 for c in selected) >= 9


def test_selection_keeps_paper_order():
    chunks = [f"c{i:03d}" for i in range(100)]

    selected = select_representative_chunks(chunks, 8, 3)

    assert selected == sorted(selected)
    assert len(selected) == len(set(selected)) == 8


def test_single_free_slot_takes_last_chunk():
    chunks = [f"c{i}" for i in range(10)]

    assert select_representative_chunks(chunks, 4, 3) == ["c0", "c1", "c2", "c9"]
