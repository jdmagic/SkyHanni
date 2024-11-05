package at.hannibal2.skyhanni.config.features.event.diana;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class DianaPartyCommandsConfig {
    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Item Count Commands", desc = "Commands to print the amount of Chimera books, " +
        "Minos Relics, Daedalus Sticks, and Griffin Feathers obtained in the current event.")
    public boolean itemCountCommands = false;

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Mob Count Commands", desc = "Commands to print the amount of total mobs and " +
        "Minos Inquisitors found in the current event.")
    public boolean mobCountCommands = false;

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Burrow Count Command", desc = "Prints the amount of burrows dug in the current event.")
    public boolean burrowCountCommand = false;

    @Expose
    @ConfigEditorBoolean
    @ConfigOption(name = "Since Command", desc = "Prints the number of dug burrows since an inquisitor, " +
        "or the amount of a certain mob since their given drop.")
    public boolean sinceCommand = false;
}
