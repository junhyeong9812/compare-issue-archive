"""Paged listing of active orders, newest first."""
from orderfeed.util import page_token


class FeedService:
    def __init__(self, store, settings):
        self.store = store
        self.settings = settings

    def page(self, cursor=None, limit=50):
        head = self.store.head_id()
        if cursor is None:
            offset, top = 0, head
        else:
            token = page_token.decode(cursor)
            page_token.ensure_current(token, head)
            offset, top = token.offset, token.top

        items = self.store.active_page(offset, limit)
        end = offset + len(items)
        if items and end < self.store.count_active():
            next_cursor = page_token.encode(page_token.PageToken(end, top))
        else:
            next_cursor = None
        return {"items": [o.to_dict() for o in items], "next_cursor": next_cursor}
