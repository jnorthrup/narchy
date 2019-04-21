package fucknutreport.config

import java.lang.Boolean.TRUE

/**
 * checks the ENV for nodeVarName. gets that from env.
 * if empty, gets that java prop.
 * if either of those two work, logs to stderr
 *
 * ... this is not idempotent, try to stuff configs in const values to be concise, and minimize the need for singleton crap.
 */

object NodeConfig {
    @JvmStatic
    val insecure = System.getProperty("insecure", "false") == TRUE.toString()


    @JvmStatic
    fun qget2(nodeVarName: String, defaultVal: String?) = get2(nodeVarName, defaultVal, true)

    @JvmStatic
    fun get2(configKey: String, defaultVal: String?, quiet: Boolean = false): String? {

        /**
         * cleans underscores in meshid. silly
         */
        val javapropname = configKey.toLowerCase().replace('_', '.')


        return (System.getenv(configKey) ?: System.getProperty(javapropname) ?: defaultVal)?.also { cfg ->
            System.setProperty(javapropname, cfg)
            if (quiet.not() || insecure) reportConfig(javapropname, cfg)
        }
    }

    @JvmStatic
    fun reportConfig(javapropname: String, `val`: String): String = `val`.apply {
        System.err.println("-D$javapropname=\"$this\"")
    }

    /**
     * typically to configIs iv var=t or var=true
     */
    @JvmStatic
    fun configIs(key: String, def: String = "true") = getK(key, def) == def

    /** java interop forces this stuttering declaration but enables infix*/
    @JvmStatic
    infix fun configIs(key: String) = key.let { configIs(it, "true") }

    /** java interop forces this stuttering declaration but enables infix*/
    @JvmStatic
    infix fun notConfig(key: String) = configIs(key, "false")

    @JvmStatic
    fun getK(key: String, default: String ) = get2(key, default, false)!!

    /**
     * defaults don't need null tests
     */
    @JvmStatic
    fun get(s: String) = get2(s, null, false)

    @JvmStatic
    fun qget(s: String) = qget2(s, null)

}




































































