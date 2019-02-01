package systems.comodal.collision.cache;


import org.junit.jupiter.api.BeforeEach;

public final class PackedCacheTest extends BaseCacheTest {

  @BeforeEach
  public void before() {
    this.maxCollisions = 8;
    this.cache = CollisionCache
        .withCapacity(32, TestNumber.class)
        .setBucketSize(maxCollisions)
        .setStoreKeys(false)
        .<TestNumber, TestNumber>setLoader(num -> num, (key, num) -> num)
        .buildPacked();
  }
}
