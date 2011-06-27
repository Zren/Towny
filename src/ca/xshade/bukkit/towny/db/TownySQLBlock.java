package ca.xshade.bukkit.towny.db;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotEmpty;

@Entity()
@Table(name = "towny_blocks")
public class TownySQLBlock {
	@Id
    private int id;
	
	private String name;
	
	@NotEmpty
	private String world;
	
	@NotEmpty
	private int x;
	
	@NotEmpty
	private int z;
	
	@NotEmpty
	private TownySQLTown town;
	
	private TownySQLResident owner;

}
