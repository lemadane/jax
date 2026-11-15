import com.lemadane.jax.JsonProperty;

public class Pagination {
    int page;
    int pageSize;
    boolean hasMore;
    String nextCursor;

    public int page() {
        return page;
    }
    @JsonProperty
    public void page(int value) {
        this.page = value;
    }

    public int pageSize() {
        return pageSize;
    }
    @JsonProperty
    public void pageSize(int value) {
        this.pageSize = value;
    }

    public boolean hasMore() {
        return hasMore;
    }
    @JsonProperty
    public void hasMore(boolean value) {
        this.hasMore = value;
    }

    public String nextCursor() {
        return nextCursor;
    }
    @JsonProperty
    public void nextCursor(String value) {
        this.nextCursor = value;
    }
}
