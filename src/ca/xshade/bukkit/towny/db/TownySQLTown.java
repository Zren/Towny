package ca.xshade.bukkit.towny.db;

import java.util.List;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 *
 * @author FuzzeWuzze
 */
@Entity()
@Table(name = "towny_towns")
public class TownySQLTown {

    @Id
    private int id;
    @NotNull
    private String playerName;
    @Length(max = 30)
    @NotEmpty
    private String name;

    @NotEmpty
    private TownySQLResident mayor;
    
    @OneToMany
    private List<TownySQLResident> assistants;
    
    @NotEmpty
    @OneToMany
    private List<TownySQLBlock> blocks;
    
    private String townBoard;
    
    @NotEmpty
    private int taxes;
    
    private int plotPrice;
    
    private int plotTax;
    
    private boolean pvp;
    
    private boolean badMobs;
    
    private boolean goodMobs;
    
    private TownySQLBlock homeBlock;
    
    private float townSpawnWorld;
    
    private float townSpawnX;
    
    private float townSpawnY;
    
    private float townSpawnZ;
    
    //resident, ally, outsider
    //build, destroy, switch, itemuse
    
    private boolean residentBuild;
    private boolean residentDestroy;
    private boolean residentSwitch;
    private boolean residentItemUse;
    private boolean allyBuild;
    private boolean allyDestroy;
    private boolean allySwitch;
    private boolean allyItemUse;
    private boolean outsiderBuild;
    private boolean outsiderDestroy;
    private boolean outsiderSwitch;
    private boolean outsiderItemUse;
}