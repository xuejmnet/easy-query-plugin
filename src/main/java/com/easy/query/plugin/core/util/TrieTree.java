package com.easy.query.plugin.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * create time 2024/2/1 10:32
 * 前缀树
 *
 * @author xuejiaming
 */

//字典树节点
class TrieTreeNode {
    //节点值
    private Character value;
    //该节点下属系列节点
    private HashMap<Character, TrieTreeNode> nexts;
    //该节点是否为某一个单词的结尾标志
    private boolean endNodeFlag;


    //初始化
    public TrieTreeNode(Character value) {
        this.value = value;
        nexts = new HashMap<>();
        this.endNodeFlag = false;
    }

    //getter和setter
    public HashMap<Character, TrieTreeNode> getNexts() {
        return nexts;
    }

    public boolean isEndNodeFlag() {
        return endNodeFlag;
    }

    public void setEndNodeFlag(boolean endNodeFlag) {
        this.endNodeFlag = endNodeFlag;
    }
}

public class TrieTree {
    //根节点值
    Character rootValue = '$';
    //根节点
    TrieTreeNode root;

    //初始化
    public TrieTree() {
        root = new TrieTreeNode(rootValue);
    }
    public TrieTree(Collection<String> dict) {
        root = new TrieTreeNode(rootValue);
        for (String val : dict) {
            insert(val);
        }
    }

    //插入
    public void insert(String word) {
        //当前节点
        TrieTreeNode nowNode = this.root;
        char[] chs = word.toCharArray();
        for (char c : chs) {
            //当前节点的下属节点中不包含c，就创建新的节点
            if (!nowNode.getNexts().containsKey(c)) {
                //创建下一个节点
                TrieTreeNode newNode = new TrieTreeNode(c);
                //将新的节点放入nownode下属节点中
                nowNode.getNexts().put(c, newNode);
            }
            //更新nownode到下一层
            nowNode = nowNode.getNexts().get(c);
        }
        //设置单词结束标志
        nowNode.setEndNodeFlag(true);
    }

    //查找
    public boolean search(String word) {
        //当前节点
        TrieTreeNode nowNode = this.root;
        char[] chs = word.toCharArray();
        for (char c : chs) {
            //当前节点的下属节点中包含c，就继续更新nownode，否则没有，返回false
            if (nowNode.getNexts().containsKey(c)) {
                nowNode = nowNode.getNexts().get(c);
            } else {
                return false;
            }
        }
        //遍历结束后，判断当前节点是否为单词结束节点
        return nowNode.isEndNodeFlag();
    }

    //匹配  与查找只有一点点不同，不用判断单词结束节点
    public boolean startsWith(String prefix) {
        TrieTreeNode nowNode = this.root;
        char[] chs = prefix.toCharArray();
        for (char c : chs) {
            //当前节点的下属节点中包含c，就继续更新nownode，否则没有，返回false
            if (nowNode.getNexts().containsKey(c)) {
                nowNode = nowNode.getNexts().get(c);
            } else {
                return false;
            }
        }
        return true;
    }
    public boolean fstMatch(String prefix) {
        TrieTreeNode nowNode = this.root;
        char[] chs = prefix.toCharArray();
        for (char c : chs) {
            TrieTreeNode trieTreeNode = fstMatch0(nowNode, c);
            if(trieTreeNode==null){
                return false;
            }
            nowNode=trieTreeNode;
        }
        return true;
    }
    private TrieTreeNode fstMatch0(TrieTreeNode nowNode,char c) {
        if (nowNode.getNexts().containsKey(c)) {
            return nowNode.getNexts().get(c);
        } else {
            for (TrieTreeNode value : nowNode.getNexts().values()) {
                TrieTreeNode trieTreeNode = fstMatch0(value, c);
                if(trieTreeNode!=null){
                    return trieTreeNode;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        String[] array = new String[]{"菜鸡", "菜狗", "张三", "张三丰满", "张三丰", "张三丰是狗", "张全", "张力", "张力懦夫", "王五", "王五王八", "王六滚蛋", "滚蛋", "滚"};
        TrieTree trieTree = new TrieTree();
        for (String s : array) {
            trieTree.insert(s);
        }
        boolean b1 = trieTree.fstMatch("张三狗");
        boolean b2 = trieTree.fstMatch("张三狗是");
        boolean b3 = trieTree.fstMatch("张三狗");
        System.out.println(b1);
        System.out.println(b2);
        System.out.println(b3);

    }
}
