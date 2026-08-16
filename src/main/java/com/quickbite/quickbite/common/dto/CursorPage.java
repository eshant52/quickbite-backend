package com.quickbite.quickbite.common.dto;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Cursor-based pagination response for UUIDv7-keyed collections.
 *
 * <h3>Why cursor pagination over offset pagination?</h3>
 * <ul>
 *   <li><b>Stable pages</b> — inserting new items never shifts existing pages.</li>
 *   <li><b>Efficient</b> — uses the primary key index: {@code WHERE id > :cursor ORDER BY id LIMIT n}.</li>
 *   <li><b>No count query</b> — no {@code SELECT COUNT(*)} overhead.</li>
 *   <li><b>Works with UUIDv7</b> — the first 48 bits encode a Unix millisecond timestamp,
 *       so lexicographic order is also chronological order.</li>
 * </ul>
 *
 * <h3>Client usage</h3>
 * <ol>
 *   <li>First request: omit {@code cursor} (or pass {@code null}).</li>
 *   <li>Subsequent requests: pass {@code cursor=<nextCursor>} from the previous response.</li>
 *   <li>Stop when {@code hasMore = false} or {@code nextCursor = null}.</li>
 * </ol>
 *
 * @param content    The items on this page (at most {@code size} entries).
 * @param nextCursor The ID of the last item on this page — pass as {@code cursor} in the next request.
 *                   {@code null} when this is the last page.
 * @param hasMore    {@code true} when there are more items after this page.
 * @param size       Actual number of items returned (maybe less than the requested size on the last page).
 */
public record CursorPage<T>(
        List<T> content,
        UUID nextCursor,
        boolean hasMore,
        int size
) {
    /**
     * Constructs a {@link CursorPage} from a list fetched with {@code requestedSize + 1} limit.
     *
     * <p>The repository must fetch <em>one extra</em> item beyond {@code requestedSize}.
     * If the extra item is present, there is a next page — it is dropped from {@code content}
     * and its ID is not exposed. The cursor is the last item actually returned.</p>
     *
     * @param fetched       Items returned by the repository (fetched with {@code requestedSize + 1}).
     * @param requestedSize The page size requested by the caller.
     * @param idExtractor   Function to extract the UUIDv7 ID from an item (used to set {@code nextCursor}).
     */
    public static <T> CursorPage<T> of(List<T> fetched, int requestedSize, Function<T, UUID> idExtractor) {
        boolean hasMore = fetched.size() > requestedSize;
        List<T> content = hasMore ? List.copyOf(fetched.subList(0, requestedSize)) : List.copyOf(fetched);
        UUID nextCursor = hasMore ? idExtractor.apply(content.getLast()) : null;
        return new CursorPage<>(content, nextCursor, hasMore, content.size());
    }
}
