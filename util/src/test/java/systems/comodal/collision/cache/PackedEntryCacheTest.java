package systems.comodal.collision.cache;

import org.junit.jupiter.api.BeforeEach;

final class PackedEntryCacheTest extends BaseEntryCacheTest {

  @BeforeEach
  public void before() {
    this.cache = CollisionCache
        .<String>withCapacity(64)
        .setLoader(BaseEntryCacheTest::hashInteger, (key, hash) -> toHexString(hash))
        .buildPacked();
  }
}
