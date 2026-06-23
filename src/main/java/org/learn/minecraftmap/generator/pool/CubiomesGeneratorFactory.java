package org.learn.minecraftmap.generator.pool;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for pooling native Cubiomes Generator pointers.
 * Allocates 27,592 bytes for the C Generator struct and handles native memory disposal.
 */
public class CubiomesGeneratorFactory extends BasePooledObjectFactory<Pointer> {

    private static final Logger logger = LoggerFactory.getLogger(CubiomesGeneratorFactory.class);
    private static final int GENERATOR_STRUCT_SIZE = 27592;

    /**
     * Subclass of JNA Memory to expose the protected dispose() method as public.
     * This allows the factory to explicitly free native C memory.
     */
    private static class PooledMemory extends Memory {
        public PooledMemory(long size) {
            super(size);
        }

        public void free() {
            super.dispose();
        }
    }

    @Override
    public Pointer create() {
        logger.debug("Allocating new native Cubiomes Generator memory block of size {}", GENERATOR_STRUCT_SIZE);
        return new PooledMemory(GENERATOR_STRUCT_SIZE);
    }

    @Override
    public PooledObject<Pointer> wrap(Pointer pointer) {
        return new DefaultPooledObject<>(pointer);
    }

    @Override
    public void destroyObject(PooledObject<Pointer> p) throws Exception {
        Pointer pointer = p.getObject();
        if (pointer instanceof PooledMemory) {
            logger.debug("Explicitly freeing native Cubiomes Generator memory pointer");
            ((PooledMemory) pointer).free();
        }
    }
}
