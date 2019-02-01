package systems.comodal.collision.cache;

import org.junit.jupiter.api.BeforeEach;

public final class SparseEntryCacheTest extends BaseEntryCacheTest {

    @BeforeEach
    public void before() {
        this.cache = CollisionCache
                .<String>withCapacity(64)
                .setLoader(BaseEntryCacheTest::hashInteger, (key, hash) -> toHexString(hash))
                .buildSparse();
    }
}
