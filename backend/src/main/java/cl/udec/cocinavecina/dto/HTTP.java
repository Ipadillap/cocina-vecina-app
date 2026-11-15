package cl.udec.cocinavecina.dto;

public final class HTTP {
    private HTTP() {}

    public record Response(int status) {
        public static final Response BAD_REQUEST = new Response(400);
        public static final Response UNAUTHORIZED = new Response(401);
        public static final Response FORBIDDEN = new Response(403);
        public static final Response NOT_FOUND = new Response(404);
        public static final Response CONFLICT = new Response(409);
        public static final Response UNPROCESSABLE_ENTITY = new Response(422);
        public static final Response TOO_MANY_REQUESTS = new Response(429);
    }
}
