package wtf.moonlight.gui.altmanager.repository.hypixel;

public class TClientResponse<Content> {

    private static final TClientResponse<?> EMPTY = new TClientResponse<>(false, null);

    private final boolean ok;
    private final Content content;

    private TClientResponse(boolean ok, Content content) {
        this.ok = ok;
        this.content = content;
    }

    public static <Content> TClientResponse<Content> of(boolean ok, Content content) {
        return new TClientResponse<>(ok, content);
    }

    public static TClientResponse<?> empty() {
        return EMPTY;
    }

    public boolean isOkay() {
        return this.ok;
    }

    public Content getContent() {
        return this.content;
    }

    @Override
    public String toString() {
        return "Response{" + "ok=" + this.ok + ", content=" + this.content + '}';
    }

}
