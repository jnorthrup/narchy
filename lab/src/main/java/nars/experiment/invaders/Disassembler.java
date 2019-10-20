package nars.experiment.invaders;

import java.util.ArrayList;

public class Disassembler {


    public static int getCode(ArrayList<String> codebuffer, int pc) {
        String code = "0x" + codebuffer.get(pc);
        code = code.toLowerCase();
        int opbytes = 1;
        switch (code) {
            case "0x00":
                System.out.println("NOP");
                break;
            case "0x01":
                System.out.println("LXI    B,#$" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0x02":
                System.out.println("STAX   B");
                break;
            case "0x03":
                System.out.println("INX    B");
                break;
            case "0x04":
                System.out.println("INR    B");
                break;
            case "0x05":
                System.out.println("DCR    B");
                break;
            case "0x06":
                System.out.println("MVI    B,#$%02x" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0x07":
                System.out.println("RLC");
                break;
            case "0x08":
                System.out.println("NOP");
                break;
            case "0x09":
                System.out.println("DAD    B");
                break;
            case "0x0a":
                System.out.println("LDAX   B");
                break;
            case "0x0b":
                System.out.println("DCX    B");
                break;
            case "0x0c":
                System.out.println("INR    C");
                break;
            case "0x0d":
                System.out.println("DCR    C");
                break;
            case "0x0e":
                System.out.println("MVI    C,#$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0x0f":
                System.out.println("RRC");
                break;

            case "0x10":
                System.out.println("NOP");
                break;
            case "0x11":
                System.out.println("LXI    D,#$" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0x12":
                System.out.println("STAX   D");
                break;
            case "0x13":
                System.out.println("INX    D");
                break;
            case "0x14":
                System.out.println("INR    D");
                break;
            case "0x15":
                System.out.println("DCR    D");
                break;
            case "0x16":
                System.out.println("MVI    D,#$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0x17":
                System.out.println("RAL");
                break;
            case "0x18":
                System.out.println("NOP");
                break;
            case "0x19":
                System.out.println("DAD    D");
                break;
            case "0x1a":
                System.out.println("LDAX   D");
                break;
            case "0x1b":
                System.out.println("DCX    D");
                break;
            case "0x1c":
                System.out.println("INR    E");
                break;
            case "0x1d":
                System.out.println("DCR    E");
                break;
            case "0x1e":
                System.out.println("MVI    E,#$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0x1f":
                System.out.println("RAR");
                break;

            case "0x20":
                System.out.println("NOP");
                break;
            case "0x21":
                System.out.println("LXI    H,#$" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0x22":
                System.out.println("SHLD   $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0x23":
                System.out.println("INX    H");
                break;
            case "0x24":
                System.out.println("INR    H");
                break;
            case "0x25":
                System.out.println("DCR    H");
                break;
            case "0x26":
                System.out.println("MVI    H,#$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0x27":
                System.out.println("DAA");
                break;
            case "0x28":
                System.out.println("NOP");
                break;
            case "0x29":
                System.out.println("DAD    H");
                break;
            case "0x2a":
                System.out.println("LHLD   $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0x2b":
                System.out.println("DCX    H");
                break;
            case "0x2c":
                System.out.println("INR    L");
                break;
            case "0x2d":
                System.out.println("DCR    L");
                break;
            case "0x2e":
                System.out.println("MVI    L,#$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0x2f":
                System.out.println("CMA");
                break;

            case "0x30":
                System.out.println("NOP");
                break;
            case "0x31":
                System.out.println("LXI    SP,#$" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0x32":
                System.out.println("STA    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0x33":
                System.out.println("INX    SP");
                break;
            case "0x34":
                System.out.println("INR    M");
                break;
            case "0x35":
                System.out.println("DCR    M");
                break;
            case "0x36":
                System.out.println("MVI    M,#$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0x37":
                System.out.println("STC");
                break;
            case "0x38":
                System.out.println("NOP");
                break;
            case "0x39":
                System.out.println("DAD    SP");
                break;
            case "0x3a":
                System.out.println("LDA    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0x3b":
                System.out.println("DCX    SP");
                break;
            case "0x3c":
                System.out.println("INR    A");
                break;
            case "0x3d":
                System.out.println("DCR    A");
                break;
            case "0x3e":
                System.out.println("MVI    A,#$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0x3f":
                System.out.println("CMC");
                break;

            case "0x40":
                System.out.println("MOV    B,B");
                break;
            case "0x41":
                System.out.println("MOV    B,C");
                break;
            case "0x42":
                System.out.println("MOV    B,D");
                break;
            case "0x43":
                System.out.println("MOV    B,E");
                break;
            case "0x44":
                System.out.println("MOV    B,H");
                break;
            case "0x45":
                System.out.println("MOV    B,L");
                break;
            case "0x46":
                System.out.println("MOV    B,M");
                break;
            case "0x47":
                System.out.println("MOV    B,A");
                break;
            case "0x48":
                System.out.println("MOV    C,B");
                break;
            case "0x49":
                System.out.println("MOV    C,C");
                break;
            case "0x4a":
                System.out.println("MOV    C,D");
                break;
            case "0x4b":
                System.out.println("MOV    C,E");
                break;
            case "0x4c":
                System.out.println("MOV    C,H");
                break;
            case "0x4d":
                System.out.println("MOV    C,L");
                break;
            case "0x4e":
                System.out.println("MOV    C,M");
                break;
            case "0x4f":
                System.out.println("MOV    C,A");
                break;

            case "0x50":
                System.out.println("MOV    D,B");
                break;
            case "0x51":
                System.out.println("MOV    D,C");
                break;
            case "0x52":
                System.out.println("MOV    D,D");
                break;
            case "0x53":
                System.out.println("MOV    D.E");
                break;
            case "0x54":
                System.out.println("MOV    D,H");
                break;
            case "0x55":
                System.out.println("MOV    D,L");
                break;
            case "0x56":
                System.out.println("MOV    D,M");
                break;
            case "0x57":
                System.out.println("MOV    D,A");
                break;
            case "0x58":
                System.out.println("MOV    E,B");
                break;
            case "0x59":
                System.out.println("MOV    E,C");
                break;
            case "0x5a":
                System.out.println("MOV    E,D");
                break;
            case "0x5b":
                System.out.println("MOV    E,E");
                break;
            case "0x5c":
                System.out.println("MOV    E,H");
                break;
            case "0x5d":
                System.out.println("MOV    E,L");
                break;
            case "0x5e":
                System.out.println("MOV    E,M");
                break;
            case "0x5f":
                System.out.println("MOV    E,A");
                break;

            case "0x60":
                System.out.println("MOV    H,B");
                break;
            case "0x61":
                System.out.println("MOV    H,C");
                break;
            case "0x62":
                System.out.println("MOV    H,D");
                break;
            case "0x63":
                System.out.println("MOV    H.E");
                break;
            case "0x64":
                System.out.println("MOV    H,H");
                break;
            case "0x65":
                System.out.println("MOV    H,L");
                break;
            case "0x66":
                System.out.println("MOV    H,M");
                break;
            case "0x67":
                System.out.println("MOV    H,A");
                break;
            case "0x68":
                System.out.println("MOV    L,B");
                break;
            case "0x69":
                System.out.println("MOV    L,C");
                break;
            case "0x6a":
                System.out.println("MOV    L,D");
                break;
            case "0x6b":
                System.out.println("MOV    L,E");
                break;
            case "0x6c":
                System.out.println("MOV    L,H");
                break;
            case "0x6d":
                System.out.println("MOV    L,L");
                break;
            case "0x6e":
                System.out.println("MOV    L,M");
                break;
            case "0x6f":
                System.out.println("MOV    L,A");
                break;

            case "0x70":
                System.out.println("MOV    M,B");
                break;
            case "0x71":
                System.out.println("MOV    M,C");
                break;
            case "0x72":
                System.out.println("MOV    M,D");
                break;
            case "0x73":
                System.out.println("MOV    M.E");
                break;
            case "0x74":
                System.out.println("MOV    M,H");
                break;
            case "0x75":
                System.out.println("MOV    M,L");
                break;
            case "0x76":
                System.out.println("HLT");
                break;
            case "0x77":
                System.out.println("MOV    M,A");
                break;
            case "0x78":
                System.out.println("MOV    A,B");
                break;
            case "0x79":
                System.out.println("MOV    A,C");
                break;
            case "0x7a":
                System.out.println("MOV    A,D");
                break;
            case "0x7b":
                System.out.println("MOV    A,E");
                break;
            case "0x7c":
                System.out.println("MOV    A,H");
                break;
            case "0x7d":
                System.out.println("MOV    A,L");
                break;
            case "0x7e":
                System.out.println("MOV    A,M");
                break;
            case "0x7f":
                System.out.println("MOV    A,A");
                break;

            case "0x80":
                System.out.println("ADD    B");
                break;
            case "0x81":
                System.out.println("ADD    C");
                break;
            case "0x82":
                System.out.println("ADD    D");
                break;
            case "0x83":
                System.out.println("ADD    E");
                break;
            case "0x84":
                System.out.println("ADD    H");
                break;
            case "0x85":
                System.out.println("ADD    L");
                break;
            case "0x86":
                System.out.println("ADD    M");
                break;
            case "0x87":
                System.out.println("ADD    A");
                break;
            case "0x88":
                System.out.println("ADC    B");
                break;
            case "0x89":
                System.out.println("ADC    C");
                break;
            case "0x8a":
                System.out.println("ADC    D");
                break;
            case "0x8b":
                System.out.println("ADC    E");
                break;
            case "0x8c":
                System.out.println("ADC    H");
                break;
            case "0x8d":
                System.out.println("ADC    L");
                break;
            case "0x8e":
                System.out.println("ADC    M");
                break;
            case "0x8f":
                System.out.println("ADC    A");
                break;

            case "0x90":
                System.out.println("SUB    B");
                break;
            case "0x91":
                System.out.println("SUB    C");
                break;
            case "0x92":
                System.out.println("SUB    D");
                break;
            case "0x93":
                System.out.println("SUB    E");
                break;
            case "0x94":
                System.out.println("SUB    H");
                break;
            case "0x95":
                System.out.println("SUB    L");
                break;
            case "0x96":
                System.out.println("SUB    M");
                break;
            case "0x97":
                System.out.println("SUB    A");
                break;
            case "0x98":
                System.out.println("SBB    B");
                break;
            case "0x99":
                System.out.println("SBB    C");
                break;
            case "0x9a":
                System.out.println("SBB    D");
                break;
            case "0x9b":
                System.out.println("SBB    E");
                break;
            case "0x9c":
                System.out.println("SBB    H");
                break;
            case "0x9d":
                System.out.println("SBB    L");
                break;
            case "0x9e":
                System.out.println("SBB    M");
                break;
            case "0x9f":
                System.out.println("SBB    A");
                break;

            case "0xa0":
                System.out.println("ANA    B");
                break;
            case "0xa1":
                System.out.println("ANA    C");
                break;
            case "0xa2":
                System.out.println("ANA    D");
                break;
            case "0xa3":
                System.out.println("ANA    E");
                break;
            case "0xa4":
                System.out.println("ANA    H");
                break;
            case "0xa5":
                System.out.println("ANA    L");
                break;
            case "0xa6":
                System.out.println("ANA    M");
                break;
            case "0xa7":
                System.out.println("ANA    A");
                break;
            case "0xa8":
                System.out.println("XRA    B");
                break;
            case "0xa9":
                System.out.println("XRA    C");
                break;
            case "0xaa":
                System.out.println("XRA    D");
                break;
            case "0xab":
                System.out.println("XRA    E");
                break;
            case "0xac":
                System.out.println("XRA    H");
                break;
            case "0xad":
                System.out.println("XRA    L");
                break;
            case "0xae":
                System.out.println("XRA    M");
                break;
            case "0xaf":
                System.out.println("XRA    A");
                break;

            case "0xb0":
                System.out.println("ORA    B");
                break;
            case "0xb1":
                System.out.println("ORA    C");
                break;
            case "0xb2":
                System.out.println("ORA    D");
                break;
            case "0xb3":
                System.out.println("ORA    E");
                break;
            case "0xb4":
                System.out.println("ORA    H");
                break;
            case "0xb5":
                System.out.println("ORA    L");
                break;
            case "0xb6":
                System.out.println("ORA    M");
                break;
            case "0xb7":
                System.out.println("ORA    A");
                break;
            case "0xb8":
                System.out.println("CMP    B");
                break;
            case "0xb9":
                System.out.println("CMP    C");
                break;
            case "0xba":
                System.out.println("CMP    D");
                break;
            case "0xbb":
                System.out.println("CMP    E");
                break;
            case "0xbc":
                System.out.println("CMP    H");
                break;
            case "0xbd":
                System.out.println("CMP    L");
                break;
            case "0xbe":
                System.out.println("CMP    M");
                break;
            case "0xbf":
                System.out.println("CMP    A");
                break;

            case "0xc0":
                System.out.println("RNZ");
                break;
            case "0xc1":
                System.out.println("POP    B");
                break;
            case "0xc2":
                System.out.println("JNZ    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xc3":
                System.out.println("JMP    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xc4":
                System.out.println("CNZ    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xc5":
                System.out.println("PUSH   B");
                break;
            case "0xc6":
                System.out.println("ADI    #$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0xc7":
                System.out.println("RST    0");
                break;
            case "0xc8":
                System.out.println("RZ");
                break;
            case "0xc9":
                System.out.println("RET");
                break;
            case "0xca":
                System.out.println("JZ     $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xcb":
                System.out.println("JMP    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xcc":
                System.out.println("CZ     $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xcd":
                System.out.println("CALL   $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xce":
                System.out.println("ACI    #$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0xcf":
                System.out.println("RST    1");
                break;

            case "0xd0":
                System.out.println("RNC");
                break;
            case "0xd1":
                System.out.println("POP    D");
                break;
            case "0xd2":
                System.out.println("JNC    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xd3":
                System.out.println("OUT    #$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0xd4":
                System.out.println("CNC    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xd5":
                System.out.println("PUSH   D");
                break;
            case "0xd6":
                System.out.println("SUI    #$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0xd7":
                System.out.println("RST    2");
                break;
            case "0xd8":
                System.out.println("RC");
                break;
            case "0xd9":
                System.out.println("RET");
                break;
            case "0xda":
                System.out.println("JC     $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xdb":
                System.out.println("IN     #$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0xdc":
                System.out.println("CC     $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xdd":
                System.out.println("CALL   $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xde":
                System.out.println("SBI    #$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0xdf":
                System.out.println("RST    3");
                break;

            case "0xe0":
                System.out.println("RPO");
                break;
            case "0xe1":
                System.out.println("POP    H");
                break;
            case "0xe2":
                System.out.println("JPO    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xe3":
                System.out.println("XTHL");
                break;
            case "0xe4":
                System.out.println("CPO    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xe5":
                System.out.println("PUSH   H");
                break;
            case "0xe6":
                System.out.println("ANI    #$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0xe7":
                System.out.println("RST    4");
                break;
            case "0xe8":
                System.out.println("RPE");
                break;
            case "0xe9":
                System.out.println("PCHL");
                break;
            case "0xea":
                System.out.println("JPE    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xeb":
                System.out.println("XCHG");
                break;
            case "0xec":
                System.out.println("CPE     $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xed":
                System.out.println("CALL    $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xee":
                System.out.println("XRI    #$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0xef":
                System.out.println("RST    5");
                break;

            case "0xf0":
                System.out.println("RP");
                break;
            case "0xf1":
                System.out.println("POP    PSW");
                break;
            case "0xf2":
                System.out.println("JP     $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xf3":
                System.out.println("DI");
                break;
            case "0xf4":
                System.out.println("CP     $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xf5":
                System.out.println("PUSH   PSW");
                break;
            case "0xf6":
                System.out.println("ORI    #$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0xf7":
                System.out.println("RST    6");
                break;
            case "0xf8":
                System.out.println("RM");
                break;
            case "0xf9":
                System.out.println("SPHL");
                break;
            case "0xfa":
                System.out.println("JM     $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xfb":
                System.out.println("EI");
                break;
            case "0xfc":
                System.out.println("CM     $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xfd":
                System.out.println("CALL   $" + codebuffer.get(pc + 2) + codebuffer.get(pc + 1));
                opbytes = 3;
                break;
            case "0xfe":
                System.out.println("CPI    #$" + codebuffer.get(pc + 1));
                opbytes = 2;
                break;
            case "0xff":
                System.out.println("RST    7");
                break;
        }
        return opbytes;
    }
}
