package jcog.io.tar;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helps dealing with file permissions.
 */
public enum PermissionUtils {
	;

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

	private static final Map<PosixFilePermission, Integer> posixPermissionToInteger = new HashMap<>();

	static {
		posixPermissionToInteger.put(PosixFilePermission.OWNER_EXECUTE, 0100);
		posixPermissionToInteger.put(PosixFilePermission.OWNER_WRITE, 0200);
		posixPermissionToInteger.put(PosixFilePermission.OWNER_READ, 0400);

		posixPermissionToInteger.put(PosixFilePermission.GROUP_EXECUTE, 0010);
		posixPermissionToInteger.put(PosixFilePermission.GROUP_WRITE, 0020);
		posixPermissionToInteger.put(PosixFilePermission.GROUP_READ, 0040);

		posixPermissionToInteger.put(PosixFilePermission.OTHERS_EXECUTE, 0001);
		posixPermissionToInteger.put(PosixFilePermission.OTHERS_WRITE, 0002);
		posixPermissionToInteger.put(PosixFilePermission.OTHERS_READ, 0004);
	}

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
			Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(f.toPath());
			int sum = posixPermissionToInteger.entrySet().stream().filter(entry -> permissions.contains(entry.getKey())).mapToInt(Map.Entry::getValue).sum();
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
        Set<StandardFilePermission> permissions = readStandardPermissions(f);
		int number = permissions.stream().mapToInt(permission -> permission.mode).sum();
		return number;
	}
}
