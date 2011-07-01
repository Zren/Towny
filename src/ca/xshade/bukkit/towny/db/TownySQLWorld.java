package ca.xshade.bukkit.towny.db;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;



@Entity()
@Table(name = "towny_worlds")
public class TownySQLWorld {
	@Id
    private int id;

    //@Length(max = 30)
    //@NotEmpty
    private String name;
    
    //@NotEmpty
    private boolean pvp;
    
    //@NotEmpty
    private boolean claimable;
    
    //@NotEmpty
    private boolean unclaimedZoneBuild;
    
    //@NotEmpty
    private boolean unclaimedZoneDestroy;
    
    //@NotEmpty
    private boolean unclaimedZoneSwitch;
    
    //@NotEmpty
    private boolean unclaimedZoneItemUse;
    
    //@NotEmpty
    private String unclaimedZoneName;
    
    private List<Integer> unclaimedZoneIgnoreIds;
    
    //@NotEmpty
    private boolean usingTowny;

}
