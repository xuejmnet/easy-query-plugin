package com.easy.query.plugin.action;

import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.persistent.EasyQueryQueryPluginConfigData;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.validator.InputValidatorImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.util.HashMap;

/**
 * create time 2024/2/1 21:14
 * 文件说明
 *
 * @author xuejiaming
 */
public class QuickTipSettingAction  extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();
        if(project==null){
            return;
        }
//        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        PsiJavaFileUtil.createAptFile(project);
        EasyQueryConfig config = EasyQueryQueryPluginConfigData.getAllEnvQuickSetting(new EasyQueryConfig());
        if(config.getConfig()==null){
            config.setConfig(new HashMap<>());
        }
        String projectName = project.getName();
        String setting = config.getConfig().get(projectName);
        if(StrUtil.isBlank(setting)){
            setting="";
            config.getConfig().put(projectName,setting);
        }
        Messages.InputDialog dialog = new Messages.InputDialog("请输入快速提示名称逗号分割,冒号分割组", "提示名称", Messages.getQuestionIcon(), setting, new InputValidatorImpl());
        dialog.show();
        String settingVal = dialog.getInputString();
        config.getConfig().put(projectName,settingVal);
        EasyQueryQueryPluginConfigData.saveAllEnvProjectQuickSetting(config);
        NotificationUtils.notifySuccess("保存成功", project);
    }
}
