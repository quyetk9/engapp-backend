"""Logic nghiệp vụ: XP, streak, ôn từ vựng (SRS), giải đấu."""
import random
from datetime import date, datetime, timedelta, timezone

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from .models import LeagueMember, LeagueRollover, User, UserVocab, Vocab

SRS_DAYS = [0, 1, 2, 4, 7, 15, 30]   # khoảng cách ôn lại theo độ thuộc 0..6
GROUP_SIZE = 30
PROMOTE_N, DEMOTE_N, MAX_TIER = 5, 6, 10


def today() -> date:
    return datetime.now(timezone.utc).date()


def week_start(d: date) -> date:
    return d - timedelta(days=d.weekday())


def calc_xp(accuracy: float, replay: bool) -> int:
    xp = 10 + round(10 * accuracy)
    return xp // 2 if replay else xp


def update_streak(user: User, d: date) -> None:
    if user.last_active_date == d:
        return
    user.streak = user.streak + 1 if user.last_active_date == d - timedelta(days=1) else 1
    user.last_active_date = d


def update_vocab(db: Session, user: User, results: list[tuple[str, bool]], d: date) -> int:
    """Cập nhật độ thuộc từ vựng; trả về số từ MỚI."""
    new = 0
    for vid, ok in results:
        if db.get(Vocab, vid) is None:
            continue
        row = db.get(UserVocab, (user.id, vid))
        if row is None:
            row = UserVocab(user_id=user.id, vocab_id=vid, strength=0, next_review=d)
            db.add(row)
            new += 1
        row.strength = min(row.strength + 1, 6) if ok else max(row.strength - 1, 0)
        row.next_review = d + timedelta(days=SRS_DAYS[row.strength])
    return new


def vocab_practice_steps(db: Session, user: User, limit: int = 8) -> list[dict]:
    """Sinh bài trắc nghiệm 'Luyện từ vựng' từ các từ người dùng đã học, ưu tiên từ đến hạn ôn."""
    rows = db.execute(
        select(UserVocab).where(UserVocab.user_id == user.id)
        .order_by(UserVocab.next_review, UserVocab.strength).limit(limit)
    ).scalars().all()
    all_vocab = list(db.scalars(select(Vocab)))
    by_id = {v.id: v for v in all_vocab}
    rng, steps = random.Random(), []
    for r in rows:
        v = by_id.get(r.vocab_id)
        if v is None:
            continue
        others = list({o.meaning_vi for o in all_vocab if o.meaning_vi != v.meaning_vi})
        if not others:
            continue
        options = rng.sample(others, min(2, len(others))) + [v.meaning_vi]
        rng.shuffle(options)
        steps.append({"type": "mcq_text", "prompt": "Dịch sang tiếng Việt", "question": v.word,
                      "options": options, "answer": options.index(v.meaning_vi),
                      "hint": None, "skippable": True, "vocab_id": v.id})
    return steps


# ---------- giải đấu ----------
def ensure_member(db: Session, user: User, d: date) -> LeagueMember:
    ws = week_start(d)
    m = db.get(LeagueMember, (user.id, ws))
    if m:
        return m
    g = LeagueMember.group_no
    open_group = db.execute(
        select(g).where(LeagueMember.week_start == ws, LeagueMember.tier == user.league_tier)
        .group_by(g).having(func.count() < GROUP_SIZE).order_by(g).limit(1)
    ).first()
    if open_group:
        group_no = open_group[0]
    else:
        mx = db.scalar(select(func.max(g)).where(LeagueMember.week_start == ws,
                                                 LeagueMember.tier == user.league_tier))
        group_no = (mx or 0) + 1
    m = LeagueMember(user_id=user.id, week_start=ws, tier=user.league_tier,
                     group_no=group_no, weekly_xp=0)
    db.add(m)
    db.flush()
    return m


def standings(db: Session, user: User, d: date) -> dict:
    me = ensure_member(db, user, d)
    rows = db.execute(
        select(LeagueMember.user_id, LeagueMember.weekly_xp, User.display_name)
        .join(User, User.id == LeagueMember.user_id)
        .where(LeagueMember.week_start == me.week_start, LeagueMember.tier == me.tier,
               LeagueMember.group_no == me.group_no)
        .order_by(LeagueMember.weekly_xp.desc(), LeagueMember.user_id)
    ).all()
    n, members, my_rank = len(rows), [], None
    use_zones = n > PROMOTE_N + DEMOTE_N
    for i, (uid, xp, name) in enumerate(rows, start=1):
        zone = "none"
        if use_zones:
            zone = "promote" if i <= PROMOTE_N else ("demote" if i > n - DEMOTE_N else "none")
        if uid == user.id:
            my_rank = i
        members.append({"rank": i, "name": name or "Người học", "weekly_xp": xp,
                        "is_me": uid == user.id, "zone": zone})
    return {"week_start": me.week_start.isoformat(), "tier": me.tier, "my_rank": my_rank,
            "promote_n": PROMOTE_N, "demote_n": DEMOTE_N, "members": members}


def rollover(db: Session, ws: date) -> int:
    """Chốt tuần `ws`: top 5 lên hạng, 6 người cuối xuống hạng. Trả về số người đổi hạng."""
    if db.get(LeagueRollover, ws):
        return 0
    groups: dict[tuple[int, int], list[LeagueMember]] = {}
    for m in db.scalars(select(LeagueMember).where(LeagueMember.week_start == ws)):
        groups.setdefault((m.tier, m.group_no), []).append(m)
    changed = 0
    for members in groups.values():
        members.sort(key=lambda m: (-m.weekly_xp, m.user_id))
        n = len(members)
        if n <= PROMOTE_N + DEMOTE_N:
            continue
        for m in members[:PROMOTE_N]:
            if m.weekly_xp > 0:
                u = db.get(User, m.user_id)
                if u.league_tier < MAX_TIER:
                    u.league_tier += 1
                    changed += 1
        for m in members[n - DEMOTE_N:]:
            u = db.get(User, m.user_id)
            if u.league_tier > 1:
                u.league_tier -= 1
                changed += 1
    db.add(LeagueRollover(week_start=ws))
    db.commit()
    return changed
