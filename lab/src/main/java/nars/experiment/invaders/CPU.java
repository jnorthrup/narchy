package nars.experiment.invaders;

public class CPU {
    private final Memory mem;

    public CPU(Memory mem) {
        this.mem = mem;
    }

    public void run() {
        var memory = mem.getMem();
        while (true) {
            var res = "0x" + toHexString((byte) memory[mem.pc]).toLowerCase();
            switch (res) {
                case "0x00":
                    print();
                    break;
                case "0x01":
                    mem.b = memory[mem.pc + 2];
                    mem.c = memory[mem.pc + 1];
                    mem.pc += 2;
                    print();
                    break;
                case "0x05": {
                    var resp = mem.b - 1;
                    mem.z = (resp == 0);
                    mem.s = (0x80 == (resp & 0x80));
                    mem.p = parity(resp, 8);
                    mem.b = resp;
                    print();
                    break;
                }
                case "0x06":
                    mem.b = memory[mem.pc + 1];
                    mem.pc++;
                    print();
                    break;
                case "0x09": {
                    var hl = (mem.h << 8) | (mem.l);
                    var bc = (mem.b << 8) | (mem.c);
                    var resp = hl + bc;
                    mem.h = (resp & 0xff00) >> 8;
                    mem.l = resp & 0xff;
                    mem.cy = ((resp & 0xffff0000) > 0);
                    print();
                    break;
                }
                case "0x0d": {
                    var resp = mem.c - 1;
                    mem.z = (resp == 0);
                    mem.s = (0x80 == (resp & 0x80));
                    mem.p = parity(resp, 8);
                    mem.c = resp;
                    print();
                    break;
                }
                case "0x0e":
                    mem.c = memory[mem.pc + 1];
                    mem.pc++;
                    print();
                    break;
                case "0x0f": {
                    var x = mem.a;
                    mem.a = ((x & 1) << 7) | (x >> 1);
                    mem.cy = (1 == (x & 1));
                    print();
                    break;
                }
                case "0x11":
                    mem.e = memory[mem.pc + 1];
                    mem.d = memory[mem.pc + 2];
                    mem.pc += 2;
                    print();
                    break;
                case "0x13":
                    mem.e++;
                    if (mem.e == 0) {
                        mem.d++;
                    }
                    print();
                    break;
                case "0x19": {
                    var hl = (mem.h << 8) | mem.l;
                    var de = (mem.d << 8) | mem.e;
                    var resp = hl + de;
                    mem.h = (resp & 0xff00) >> 8;
                    mem.l = resp & 0xff;
                    mem.cy = ((resp & 0xffff0000) != 0);
                    print();
                    break;
                }
                case "0x1a": {
                    var offset = (mem.d << 8) | mem.e;
                    mem.a = memory[offset];
                    print();
                    break;
                }
                case "0x21":
                    mem.l = memory[mem.pc + 1];
                    mem.h = memory[mem.pc + 2];
                    mem.pc += 2;
                    print();
                    break;
                case "0x23":
                    mem.l++;
                    if (mem.l == 0) {
                        mem.h++;
                    }
                    print();
                    break;
                case "0x26":
                    mem.h = memory[mem.pc + 1];
                    mem.pc++;
                    print();
                    break;
                case "0x29":
                    var hl = (mem.h << 8) | mem.l;
                    var resp = hl + hl;
                    mem.h = (resp & 0xff00) >> 8;
                    mem.l = resp & 0xff;
                    mem.cy = ((resp & 0xffff0000) != 0);
                    print();
                    break;
                case "0x31":
                    mem.sp = (memory[mem.pc + 2] << 8) | memory[mem.pc + 1];
                    mem.pc += 2;
                    print();
                    break;
                case "0x32": {
                    var offset = (memory[mem.pc + 2] << 8) | (memory[mem.pc + 1]);
                    memory[offset] = mem.a;
                    mem.pc += 2;
                    print();
                    break;
                }
                case "0x36": {
                    var offset = (mem.h << 8) | mem.l;
                    memory[offset] = memory[mem.pc + 1];
                    mem.pc++;
                    print();
                    break;
                }
                case "0x3a": {
                    var offset = (memory[mem.pc + 2] << 8) | (memory[mem.pc + 1]);
                    mem.a = memory[offset];
                    mem.pc += 2;
                    print();
                    break;
                }
                case "0x3e":
                    mem.a = memory[mem.pc + 1];
                    mem.pc++;
                    print();
                    break;
                case "0x56": {
                    var offset = (mem.h << 8) | (mem.l);
                    mem.d = memory[offset];
                    print();
                    break;
                }
                case "0x5e": {
                    var offset = (mem.h << 8) | (mem.l);
                    mem.e = memory[offset];
                    print();
                    break;
                }
                case "0x66": {
                    var offset = (mem.h << 8) | (mem.l);
                    mem.h = memory[offset];
                    print();
                    break;
                }
                case "0x6f":
                    mem.l = mem.a;
                    print();
                    break;
                case "0x77": {
                    var offset = (mem.h << 8) | (mem.l);
                    memory[offset] = mem.a;
                    print();
                    break;
                }
                case "0x7a":
                    mem.a = mem.d;
                    print();
                    break;
                case "0x7b":
                    mem.a = mem.e;
                    print();
                    break;
                case "0x7c":
                    mem.a = mem.h;
                    print();
                    break;
                case "0x7e":
                    var offset = (mem.h << 8) | (mem.l);
                    mem.a = memory[offset];
                    print();
                    break;
                case "0xa7":
                    mem.a &= mem.a;
                    LogicFlags();
                    print();
                    break;
                case "0xaf":
                    mem.a ^= mem.a;
                    LogicFlags();
                    print();
                    break;
                case "0xc1":
                    mem.c = memory[mem.sp];
                    mem.b = memory[mem.sp + 1];
                    mem.sp += 2;
                    print();
                    break;
                case "0xc2":
                    if (mem.z == false)
                        mem.pc = (memory[mem.pc + 2] << 8) | memory[mem.pc + 1];
                    else
                        mem.pc += 2;
                    print();
                    break;
                case "0xc3":
                    mem.pc = (memory[mem.pc + 2] << 8) | memory[mem.pc + 1];
                    print();
                    break;
                case "0xc5":
                    memory[mem.sp - 1] = mem.b;
                    memory[mem.sp - 2] = mem.c;
                    mem.sp -= 2;
                    print();
                    break;
                case "0xc6": {
                    var x = mem.a + memory[mem.pc + 1];
                    mem.z = ((x & 0xff) == 0);
                    mem.s = (0x80 == (x & 0x80));
                    mem.p = parity((x & 0xff), 8);
                    mem.cy = (x > 0xff);
                    mem.a = x;
                    mem.pc++;
                    print();
                    break;
                }
                case "0xc9":
                    mem.pc = memory[mem.sp] | (memory[mem.sp + 1] << 8);
                    mem.sp += 2;
                    print();
                    break;
                case "0xcd":
                    var ret = mem.pc + 2;
                    memory[mem.sp - 1] = (ret >> 8) & 0xff;
                    memory[mem.sp - 2] = (ret & 0xff);
                    mem.sp -= 2;
                    mem.pc = (memory[mem.pc + 2] << 8) | memory[mem.pc + 1];
                    print();
                    break;
                case "0xd1":
                    mem.e = memory[mem.sp];
                    mem.d = memory[mem.sp + 1];
                    mem.sp += 2;
                    print();
                    break;
                case "0xd3":
                    mem.pc++;
                    print();
                    break;
                case "0xd5":
                    memory[mem.sp - 1] = mem.d;
                    memory[mem.sp - 2] = mem.e;
                    mem.sp -= 2;
                    print();
                    break;
                case "0xe1":
                    mem.l = memory[mem.sp];
                    mem.h = memory[mem.sp + 1];
                    mem.sp += 2;
                    print();
                    break;
                case "0xe5":
                    memory[mem.sp - 1] = mem.h;
                    memory[mem.sp - 2] = mem.l;
                    mem.sp -= 2;
                    print();
                    break;
                case "0xe6":
                    mem.a &= memory[mem.pc + 1];
                    LogicFlags();
                    mem.pc++;
                    print();
                    break;
                case "0xeb":
                    var save1 = mem.d;
                    var save2 = mem.e;
                    mem.d = mem.h;
                    mem.e = mem.l;
                    mem.h = save1;
                    mem.l = save2;
                    print();
                    break;
                case "0xf1":
                    mem.a = memory[mem.sp + 1];
                    var psw = memory[mem.sp];
                    mem.z = (0x01 == (psw & 0x01));
                    mem.s = (0x02 == (psw & 0x02));
                    mem.p = (0x04 == (psw & 0x04));
                    mem.cy = (0x05 == (psw & 0x08));
                    mem.ac = (0x10 == (psw & 0x10));
                    mem.sp += 2;
                    print();
                    break;
                case "0xf5":


                    print();
                    break;
                case "0xfb":
                    mem.int_enable = 1;
                    print();
                    break;
                case "0xfe":
                    var x = mem.a - memory[mem.pc + 1];
                    mem.z = (x == 0);
                    mem.s = (0x80 == (x & 0x80));
                    mem.p = parity(x, 8);
                    mem.cy = (mem.a < memory[mem.pc + 1]);
                    mem.pc++;
                    print();
                    break;
                default:
                    UnimplementedInstruction();
            }
            mem.pc++;
        }
    }

    public void print() {
        System.out.println("Condition Codes: cy: " + mem.cy + " p: " + mem.p + " s: " + mem.s + " z: " + mem.z);
        System.out.println("Registers: A: " + toHexString((byte) mem.a) + " B: " + toHexString((byte) mem.b) + " C: " + toHexString((byte) mem.c) +
                " D: " + toHexString((byte) mem.d) + " E: " + toHexString((byte) mem.e) + " H: " + toHexString((byte) mem.h) +
                " L: " + toHexString((byte) mem.l) + " SP: " + toHexString((byte) mem.sp));
        System.out.println();
    }

    public static boolean parity(int x, int size) {
        x &= ((1 << size) - 1);
        var p = 0;
        for (var i = 0; i < size; i++) {
            if ((x & 0x1) != 0) {
                p++;
            }
            x >>= 1;
        }
        return (0 == (p & 0x1));
    }

    public static void UnimplementedInstruction() {
        System.out.println("Error: Unimplemented instruction\n");
        System.exit(0);
    }

    public static String toHexString(byte b) {
        return String.format("%02X", b);
    }

    public void LogicFlags() {
        mem.cy = mem.ac = false;
        mem.z = (mem.a == 0);
        mem.s = (0x80 == (mem.a & 0x80));
        mem.p = parity(mem.a, 8);
    }
}
