package ca.xshade.bukkit.towny.db;


import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name = "towny_residents")

public class TownySQLResident {
	@Id
    private int id;
    
    @Length(max = 30)
    @NotEmpty
    private String name;

    @NotEmpty
    private int lastOnline;
    
    @NotEmpty
    private int registered;
    
    private TownySQLTown town;
    
    @ManyToMany(mappedBy="owner")
    private List<TownySQLResident> friends;
    
    //Protection status?
}
