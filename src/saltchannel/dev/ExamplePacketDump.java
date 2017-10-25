package saltchannel.dev;

import saltchannel.util.CryptoTestData;
import saltchannel.util.Hex;
import saltchannel.v2.packets.*;

public class ExamplePacketDump {

    public static void main(String[] args) {
        new ExamplePacketDump().M1();
        new ExamplePacketDump().M2();
        new ExamplePacketDump().M3();        
        new ExamplePacketDump().M4();                
    }
    
    public ExamplePacketDump() {
    }
    
    private void m1_print(int case_id, M1Message m1) {
        String sigkey_str = m1.serverSigKey != null? "+" : "null";
        String tik_str = m1.ticket != null? Hex.create(m1.ticket) : "null";
        System.out.println(String.format("-------------\nM1 CASE: %d -> time:  0x%x, serverSigKey: %s, ticket: %s, ticketRequested: %b ", 
                                       case_id, m1.time, sigkey_str, tik_str, m1.ticketRequested));        
        System.out.println("\nsizeof(M1): " + m1.getSize() + ", M1: '" + Hex.create(m1.toBytes())+"'\n");
    }

    private void M1() {
        M1Message m1;

         // CASE 1 - NO ticket, NO server sig key, NO ticketRequested
         m1 = new M1Message();
         m1.time = 0xdeadbeef;
         m1.clientEncKey = CryptoTestData.aEnc.pub();
         m1.serverSigKey = null;
         m1.ticket = null;
         m1.ticketRequested = false;
                
         m1_print(1, m1);
         m1 = null;       

         // CASE 2 - NO ticket, + server sig key,  + ticketRequested
         m1 = new M1Message();
         m1.time = 0xdeadbeef;
         m1.clientEncKey = CryptoTestData.aEnc.pub();
         m1.serverSigKey = CryptoTestData.bSig.pub();
         m1.ticket = null;
         m1.ticketRequested = true;
                
         m1_print(2, m1);     
         m1 = null;                

         // CASE 3 - + Ticket, NO server sig key:  + ticketRequested
         m1 = new M1Message();
         m1.time = 0xdeadbeef;
         m1.clientEncKey = CryptoTestData.aEnc.pub();
         m1.serverSigKey = null;
         m1.ticket = new byte[127];
         for (int i = 0; i<127; i++)
            m1.ticket[i] = (byte)i;
         m1.ticketRequested = true;
                
         m1_print(3, m1);
         m1 = null;      

         // CASE 4 - + Ticket, + server sig key:  + ticketRequested
         m1 = new M1Message();
         m1.time = 0xdeadbeef;
         m1.clientEncKey = CryptoTestData.aEnc.pub();
         m1.serverSigKey = CryptoTestData.bSig.pub();
         m1.ticket = new byte[127];
         for (int i = 0; i<127; i++)
            m1.ticket[i] = (byte)i;
         m1.ticketRequested = true;
                
         m1_print(4, m1);
         m1 = null;                                          
    }
    
    private void m2_print(int case_id, M2Message m2) {
        String enckey_str = m2.serverEncKey != null? "+" : "null";
        System.out.println(String.format("-------------\nM2 CASE: %d -> time:  0x%x, ServerEncKey: %s, resumeSupported: %b ", 
                                       case_id, m2.time, enckey_str,  m2.resumeSupported));        
        System.out.println("\nsizeof(M2): " + m2.getSize() + ", M2: '" + Hex.create(m2.toBytes())+"'\n");
    }

  private void M2() {
         M2Message m2;

         m2 = new M2Message();
         m2.time = 0xdeadbeef;
         m2.resumeSupported = true;
         m2.serverEncKey = CryptoTestData.bEnc.pub();
                
         m2_print(1, m2);
    }


    private void m3_print(int case_id, M3Packet m3) {
        System.out.println(String.format("-------------\nM3 CASE: %d -> time:  0x%x, ServerEncKey: bSig.pub", 
                                       case_id, m3.time));        
        System.out.println("\nsizeof(M3): " + m3.getSize() + ", M3: '" + Hex.create(m3.toBytes())+"'\n");
    }

  private void M3() {
         M3Packet m3;

         m3 = new M3Packet();
         m3.time = 0xdeadbeef;
         m3.serverSigKey = CryptoTestData.bSig.pub();
         m3.signature1 = Hex.toBytes("be3552a308cd05afd2943030a5a582259875d00ab313a7f6d8a8fc6bf3af4732491cbc6d62351b396c8121a077e739f7764992f30be24a9b25ddedc3d68388c6");
         m3_print(1, m3);
    }

  private void m4_print(int case_id, M4Packet m4) {
        System.out.println(String.format("-------------\nM4 CASE: %d -> time:  0x%x, ClientSigKey: aSig.pub", 
                                       case_id, m4.time));        
        System.out.println("\nsizeof(M4): " + m4.getSize() + ", M4: '" + Hex.create(m4.toBytes())+"'\n");
    }

  private void M4() {
         M4Packet m4;

         m4 = new M4Packet();
         m4.time = 0xdeadbeef;
         m4.clientSigKey = CryptoTestData.aSig.pub();
         m4.signature2 = Hex.toBytes("be3552a308cd05afd2943030a5a582259875d00ab313a7f6d8a8fc6bf3af4732491cbc6d62351b396c8121a077e739f7764992f30be24a9b25ddedc3d68388c6");
         m4_print(1, m4);
    }

}