package nars.op;

import com.google.common.base.Splitter;
import jcog.TODO;
import jcog.WTF;
import nars.$;
import nars.term.Term;
import nars.term.atom.Atom;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.util.WeakRefFileListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.util.List;

import static nars.Op.INH;
import static nars.Op.PROD;

/** File and URL access interface (through VFS) */
public class FileFunc {

    static FILE get(String path) throws FileSystemException {
        return new FILE(fs.resolveFile(path));
    }

    static FILE get(URL url) throws FileSystemException {
        return new FILE(fs.resolveFile(url));
    }

    @Nullable
    static FILE getOrNull(String path)  {
        try {
            return get(path);
        } catch (FileSystemException e) {
            return null;
        }
    }
    @Nullable
    static FILE getOrNull(URL path)  {
        try {
            return get(path);
        } catch (FileSystemException e) {
            return null;
        }
    }

    public static Term the(URL u) {

        if (u.getQuery()!=null)
            throw new TODO();

        String schemeStr = u.getProtocol();
        String authorityStr = u.getAuthority();
        String pathStr = u.getPath();

        return URI(schemeStr, authorityStr, pathStr);
    }

    public static Term the(URI u) {

        if (u.getFragment()!=null || u.getQuery()!=null)
            throw new TODO();

        String schemeStr = u.getScheme();
        String authorityStr = u.getAuthority();
        String pathStr = u.getPath();

        return URI(schemeStr, authorityStr, pathStr);
    }

    /** https://en.wikipedia.org/wiki/Uniform_Resource_Identifier
     *
     * the URI scheme becomes the inheritance subject of the operation. so path components are the pred as a product. query can be the final component wrapped in a set to distinguish it, and init can be a json-like set of key/value pairs. the authority username/password can be special fields in that set of pairs.
     *
     * TODO authority, query
     * */
    public static Term URI(String schemeStr, @Nullable String authority, String pathStr) {
        /*
        URI = scheme:[//authority]path[?query][#fragment]
        authority = [userinfo@]host[:port]

                  userinfo     host        port
          ┌─┴─┬──┴────┬┴┐
          https://john.doe@www.example.com:123/forum/questions/?tag=networking&order=newest#top
         └┬┘└──────────────┴────────┴───────┬────-┴┬┘
         scheme           authority                 path                   query          fragment

          ldap://[2001:db8::7]/c=GB?objectClass?one
          └─┬┘ └───────┬─────┘└─┬─┘ └──────┬──────┘
         scheme    authority  path       query

          mailto:John.Doe@example.com
          └──┬─┘ └─────────┬────────┘
          scheme         path

          news:comp.infosystems.www.servers.unix
          └─┬┘ └───────────────┬───────────────┘
         scheme              path

          tel:+1-816-555-1212
          └┬┘ └──────┬──────┘
        scheme     path

          telnet://192.0.2.16:80/
          └──┬─┘ └──────┬──────┘│
          scheme    authority  path

          urn:oasis:names:specification:docbook:dtd:xml:4.1.2
          └┬┘ └──────────────────────┬──────────────────────┘
        scheme                     path

        */

        Atom scheme = (Atom) $.the(schemeStr); //TODO cache these commonly used

        //TODO use more reliable path parser
        List<String> pathComponents = Splitter.on('/').omitEmptyStrings().splitToList(pathStr);

        Term path = $.p((String[])(pathComponents.toArray(ArrayUtils.EMPTY_STRING_ARRAY)));
        return (authority == null || authority.isEmpty()) ?
                $.inh(path, scheme) : $.inh( PROD.the(INH.the(path, /*TODO parse*/$.the(authority))), scheme);
    }


    public static final FileSystemManager fs;
    static {
        try {
            fs = VFS.getManager();
        } catch (FileSystemException e) {
            throw new WTF(e);
        }
    }



    static final Logger logger = LoggerFactory.getLogger(FileFunc.class);

    public static final class FILE {

        public final FileObject file;

        public FILE(FileObject file) {
            this.file = file;
        }

        public void on(FileListener l) {
            on(l, true);
        }

        public void on(FileListener l, boolean includeExistingInitially) {

            WeakRefFileListener.installListener(file, l);

            if (includeExistingInitially) {
                try {
                    //invoke listener with existing children
                    for (FileObject c : file.getChildren()) {
                        try {
                            l.fileCreated(new FileChangeEvent(c));
                        } catch (Exception e) {
                            logger.error("{} {} {}", file, c, e);
                        }
                    }
                } catch (FileSystemException e) {
                    logger.error("{} {} {}", file, e);
                }
            }
        }
    }


}
