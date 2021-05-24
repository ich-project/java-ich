package org.ich.common.common.overlay.discover.node;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class DBNode {

  @Getter
  @Setter
  private List<DBNodeStats> nodes = new ArrayList<>();

}
