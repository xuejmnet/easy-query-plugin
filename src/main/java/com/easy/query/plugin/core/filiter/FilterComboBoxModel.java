package com.easy.query.plugin.core.filiter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * create time 2023/11/30 13:28
 * 文件说明
 *
 * @author xuejiaming
 */
public class FilterComboBoxModel extends AbstractListModel<String> implements ComboBoxModel<String> {
    private List<String> items;
    private List<String> filteredItems;
    private String selectedItem;

    public FilterComboBoxModel(List<String> items,int selectIndex) {
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
        if(selectIndex<0){
            this.selectedItem=items.get(0);
        }
    }

    public void filterItems(String filterText) {
        filteredItems.clear();
        for (String item : items) {
            if (item.toLowerCase().contains(filterText.toLowerCase())) {
                filteredItems.add(item);
            }
        }
        fireContentsChanged(this, 0, getSize());
    }

    @Override
    public int getSize() {
        return filteredItems.size();
    }

    @Override
    public String getElementAt(int index) {
        return filteredItems.get(index);
    }

    @Override
    public void setSelectedItem(Object anItem) {
        String anItem1 = (String) anItem;
        if(items.contains(anItem1)){
            selectedItem = anItem1;
        }
    }

    @Override
    public Object getSelectedItem() {
        return selectedItem;
    }

}