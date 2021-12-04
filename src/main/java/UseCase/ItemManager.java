package UseCase;

import Entities.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager implements Serializable{
    private final Map<String, Item> imap = new HashMap<String, Item>();
    private final ItemStorer storer;
    private final ItemSearcher searcher;
    private final ItemPicker picker;
    private final ItemTimer timer;

    /**
     * A new ItemManager, with a preset series of containers.
     * This is the "Facade" class.
     */
    public ItemManager(ItemStorer storer, ItemSearcher searcher, ItemPicker picker, ItemTimer timer) throws IOException {
        this.storer = storer;
        this.searcher = searcher;
        this.picker = picker;
        this.timer = timer;
        Containerfacotry cf = new Containerfacotry();
        Locker l = (Locker)cf.get_container("Locker");
        Freezer f = (Freezer)cf.get_container("Freezer");
        Refrigerator r = (Refrigerator)cf.get_container("Refrigerator");
        storer.setup(imap, l, f, r);
        picker.setup(imap, l, f, r);
    }

    /**
     * Reload the information, when files are read.
     * @param lmap the map containing information of the locker
     * @param fmap the map containing information of the freezer
     * @param rmap the map containing information of the refrigerator
     */
    public void reload(Map<String, Boolean> lmap, Map<String, Boolean> fmap, Map<String, Boolean> rmap){
        Locker l = new Locker(lmap);
        Freezer f = new Freezer(fmap);
        Refrigerator r = new Refrigerator(rmap);

        storer.setup(imap,l,f,r);
        picker.setup(imap,l,f,r);
    }

    public Map<String, Item> getItemMap() {
        return imap;
    }

    /**
     * Add an item, with its info, to the containers based on its storage requirement.
     * @param id the identification number of this item.
     * @param info the information attached to this item.
     * @param storageRequirement one of the strings "L/F/R". This identifies which container it needs
     * @param currentUser the user's username, of whom that adds this item.
     * @return return the location where the item is stored; if an item of the same id already exists, return a special
     * string "*"; if the item cannot be stored, return null.
     * @throws IOException
     */
    public String addItem(String id, List<String> info, String storageRequirement, String currentUser) throws IOException {
        Item i = storer.create(id, info, storageRequirement);
        if(i == null){return "*";}
        timer.RecordStart(i);
        return storer.add(id,currentUser);
    }

    /**
     * Remove an item with id.
     * @param id the identification number of the item to be removed.
     * @return return the location of the item; if the item is not found, return null.
     */
    public String removeItem(String id) {
        if(searcher.search(id,imap)!=null){
            timer.RecordEnd(id);
            return picker.remove(id);}
        else{
            return null;}
    }

    /**
     * Search for an item with id.
     * @param id the identification number of the item to be searched.
     * @return return the information of the item fund, in a list; if the item is not fund, return null.
     */
    public List<String> searchItem(String id){
        List<String> i_list = searcher.search(id, imap);
        if(i_list==null){return null;}
        List<String> t_list = checkFee(id);
        i_list.addAll(t_list);
        return i_list;
    }

    /**
     * Check the storage fee for a item with given id.
     * @param id the identification number of the item.
     * @return Return a list of information with the item id:
     * index 0 - the time when it was stored
     * index 1 - the expiration date of the free storage
     * index 2 - its current storage fee.
     */
    public List<String> checkFee(String id){
        List<String> info = new ArrayList<>();
        List<String> tls = timer.getTimeListString(id);
        String fee = String.valueOf(timer.CalculateFee(id));
        info.add(0, tls.get(0));
        info.add(1,tls.get(1));
        info.add(2, fee);
        return info;
    }

}
