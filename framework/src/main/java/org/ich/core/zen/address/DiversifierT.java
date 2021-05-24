package org.ich.core.zen.address;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.ich.common.common.zksnark.JLibrustzcash;
import org.ich.core.Constant;
import org.ich.core.exception.ZksnarkException;
import org.ich.keystore.Wallet;

@AllArgsConstructor
public class DiversifierT {

  @Setter
  @Getter
  private byte[] data = new byte[Constant.ZC_DIVERSIFIER_SIZE];

  public DiversifierT() {
  }

  public static DiversifierT random() throws ZksnarkException {
    byte[] d;
    while (true) {
      d = Wallet.generateRandomBytes(Constant.ZC_DIVERSIFIER_SIZE);
      if (JLibrustzcash.librustzcashCheckDiversifier(d)) {
        break;
      }
    }
    return new DiversifierT(d);
  }
}
