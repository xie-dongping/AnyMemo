/*
Copyright (C) 2010 Haowen Ning

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package org.liberty.android.fantastischmemo.cardscreen;

import org.liberty.android.fantastischmemo.*;

import org.amr.arabic.ArabicUtilities;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URL;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Date;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.content.Context;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.ClipboardManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.gesture.GestureOverlayView;
import android.widget.Button;
import android.os.Handler;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.util.Log;
import android.os.SystemClock;
import android.os.Environment;
import android.graphics.Typeface;
import android.content.res.Configuration;
import android.view.inputmethod.InputMethodManager;
import android.database.SQLException;

class ItemQueueManager{
    private Context mContext;
    private String dbPath;
    private String dbName;
    private DatabaseHelper dbHelper;
    private String activeFilter = "";
    private int queueSize = 10;
    private ArrayList<Item> learnQueue = null;

    public ItemQueueManager(Context context, String dbpath, String dbname) throws SQLException{
        mContext = context;
        dbPath = dbpath;
        dbName = dbname;
        dbHelper = new DatabaseHelper(context, dbpath, dbname);
    }

    public void setFilter(String filter){
        activeFilter = filter;
    }

    public void setQueueSize(int sz){
        queueSize = sz;
    }

    public boolean initQueue(){
        learnQueue = dbHelper.getListItems(1, queueSize, 4, activeFilter);
        if(learnQueue == null || learnQueue.size() == 0){
            return false;
        }
        else{
            return true;
        }
    }

    /*
     * update current item and remove it in the queue
     * if the current item is not learned, this method will put it at the 
     * end of the queue. If not, this method will pull another new card from
     * the database
     * The item parameter is the null, it will return current head of queue
     */
    public Item updateAndNext(Item item){
        if(learnQueue == null || learnQueue.size() == 0){
            return null;
        }
        if(item == null){
            return learnQueue.get(0);
        }
        Item orngItem = learnQueue.remove(0);
        if(item.isScheduled() || item.isNew()){
            learnQueue.add(orngItem);
        }
        else{
            dbHelper.addOrReplaceItem(item);
        }
        /* Fill up the queue to its queue size */
        int maxNewId = getMaxQueuedItemId(true);
        int maxRevId = getMaxQueuedItemId(false);
        boolean fetchRevFlag = true;
        /* New item in database */
        while(learnQueue.size() < queueSize){
            if(fetchRevFlag == true){
                Item newItemFromDb = dbHelper.getItemById(maxRevId + 1, 2, true, activeFilter);
                if(newItemFromDb == null){
                    fetchRevFlag = false;
                }
                else{
                    learnQueue.add(newItemFromDb);
                    maxRevId = newItemFromDb.getId();
                }
            }
            else{
                Item newItemFromDb = dbHelper.getItemById(maxNewId + 1, 1, true, activeFilter);
                if(newItemFromDb != null){
                    learnQueue.add(newItemFromDb);
                    maxNewId = newItemFromDb.getId();
                }
                else{
                    break;
                }
            }
        }
        if(learnQueue.size() > 0){
            return learnQueue.get(0);
        }
        else{
            return null;
        }
    }

    private int getMaxQueuedItemId(boolean isNewItem){
        if(learnQueue == null){
            throw new IllegalStateException("Learning queue is empty");
        }
        int maxId = -1;
        int id = -1;
        for(Item item : learnQueue){
            id = item.getId();
            if(id > maxId && (isNewItem ? item.isNew() : !item.isNew())){
                maxId = id;
            }
        }
        return maxId;
    }


}

