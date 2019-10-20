package jcog.io.tar;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import static java.nio.file.attribute.PosixFilePermission.*;

/**
 * Helps dealing with file permissions.
 */
public class PermissionUtils {

	/**
	 * XXX: When using standard Java permissions, we treat 'owner' and 'group' equally and give no
	 *      permissions for 'others'.
	 */
	private enum StandardFilePermission {
		EXECUTE(0110), WRITE(0220), READ(0440);

		private final int mode;

		StandardFilePermission(int mode) {
			this.mode = mode;
		}
	}

	private static final Map<PosixFilePermission, Integer> posixPermissionToInteger = new EnumMap<>(PosixFilePermission.class){{
	 put(OWNER_EXECUTE, 0100);
	 put(OWNER_WRITE, 0200);
	 put(OWNER_READ, 0400);

	 put(GROUP_EXECUTE, 0010);
	 put(GROUP_WRITE, 0020);
	 put(GROUP_READ, 0040);

	 put(OTHERS_EXECUTE, 0001);
	 put(OTHERS_WRITE, 0002);
	 put(OTHERS_READ, 0004);
	}};



	/**
	 * Get file permissions in octal mode, e.g. 0755.
	 *
	 * Note: it uses `java.nio.file.attribute.PosixFilePermission` if OS supports this, otherwise reverts to
	 * using standard Java file operations, e.g. `java.io.File#canExecute()`. In the first case permissions will
	 * be precisely as reported by the OS, in the second case 'owner' and 'group' will have equal permissions and
	 * 'others' will have no permissions, e.g. if file on Windows OS is `read-only` permissions will be `0550`.
	 *
	 * @throws NullPointerException if file is null.
	 * @throws IllegalArgumentException if file does not exist.
	 */
	public static int permissions(File f) {
		if(f == null) {
			throw new NullPointerException("File is null.");
		}
		if(!f.exists()) {
			throw new IllegalArgumentException("File " + f + " does not exist.");
		}

		return isPosix ? posixPermissions(f) : standardPermissions(f);
	}

	private static final boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

	private static int posixPermissions(File f) {
		int number;
		try {
			var permissions = Files.getPosixFilePermissions(f.toPath());
			var sum = posixPermissionToInteger.entrySet().stream().filter(entry -> permissions.contains(entry.getKey())).mapToInt(Map.Entry::getValue).sum();
			number = sum;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return number;
	}

	private static Set<StandardFilePermission> readStandardPermissions(File f) {
		Set<StandardFilePermission> permissions = EnumSet.noneOf(StandardFilePermission.class);
		if(f.canExecute()) {
			permissions.add(StandardFilePermission.EXECUTE);
		}
		if(f.canWrite()) {
			permissions.add(StandardFilePermission.WRITE);
		}
		if(f.canRead()) {
			permissions.add(StandardFilePermission.READ);
		}
		return permissions;
	}

	private static Integer standardPermissions(File f) {
		var permissions = readStandardPermissions(f);
		var number = permissions.stream().mapToInt(permission -> permission.mode).sum();
		return number;
	}
}
