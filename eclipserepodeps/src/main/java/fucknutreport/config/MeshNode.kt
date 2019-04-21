package fucknutreport.config

import java.io.FileInputStream
import java.nio.file.Paths

object MeshNode {
//    val hzConfig by lazy { Config(System.getProperty("mesh.id", "testnet")) }
//    val hz by lazy { Hazelcast.getOrCreateHazelcastInstance(hzConfig) }
//    fun meshOp() = hz

    /**
     * use this is for stateless nodes
     */
    fun main(vararg args: String) {
        FileInputStream(args.firstOrNull() ?: Paths.get(
                "cfg", "defaults.ini").toAbsolutePath().toString()).use{
            System.getProperties().load(it)
        }
    }
}
