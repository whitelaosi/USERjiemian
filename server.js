const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = 8000;

// 中间件
app.use(cors());
app.use(bodyParser.json({ limit: '50mb' }));
app.use(express.static(path.join(__dirname, 'public')));

// API路由 - 获取表格数据
app.get('/api/tables', (req, res) => {
  try {
    const data = fs.readFileSync(path.join(__dirname, 'public', 'data', 'tables.json'));
    res.json(JSON.parse(data));
  } catch (error) {
    console.error('读取表格数据失败:', error);
    res.status(500).json({ error: '读取表格数据失败' });
  }
});

// API路由 - 保存表格数据
app.post('/api/tables', (req, res) => {
  try {
    const data = JSON.stringify(req.body, null, 2);
    fs.writeFileSync(path.join(__dirname, 'public', 'data', 'tables.json'), data);
    res.json({ success: true, message: '表格数据保存成功' });
  } catch (error) {
    console.error('保存表格数据失败:', error);
    res.status(500).json({ error: '保存表格数据失败' });
  }
});
// API路由 - 重新生成特定页面的表格
// API路由 - 重新生成特定页面的表格
app.post('/api/regenerate-table', (req, res) => {
  try {
    const { page, exclude_merge } = req.body;

    if (!page) {
      return res.status(400).json({ success: false, message: "未指定页码" });
    }

    console.log(`开始处理第${page}页表格重新生成请求...`);

    // 获取当前配置信息，包括PDF路径
    const configPath = path.join(__dirname, 'public', 'data', 'config.json');
    let pdfPath = '';
    let scriptPath = '';

    if (fs.existsSync(configPath)) {
      const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
      pdfPath = config.pdfPath || '';
      scriptPath = config.scriptPath || '';
      console.log(`从配置读取PDF路径: ${pdfPath}`);
      console.log(`从配置读取脚本路径: ${scriptPath}`);
    } else {
      console.error('配置文件不存在:', configPath);
      return res.status(400).json({ success: false, message: "找不到配置文件" });
    }

    if (!pdfPath || !fs.existsSync(pdfPath)) {
      console.error('PDF文件不存在或路径无效:', pdfPath);
      return res.status(400).json({ success: false, message: "找不到PDF文件或路径无效" });
    }

    // 备份当前表格数据 - 简单方法，创建一个临时备份
    const tablesPath = path.join(__dirname, 'public', 'data', 'tables.json');
    const backupPath = path.join(__dirname, 'public', 'data', 'tables_backup.json');

    // 确保我们有原始数据
    if (fs.existsSync(tablesPath)) {
      // 读取原始数据
      const originalData = JSON.parse(fs.readFileSync(tablesPath, 'utf8'));
      // 保存备份
      fs.writeFileSync(backupPath, JSON.stringify(originalData), 'utf8');
      console.log(`已备份当前表格数据，包含 ${originalData.tables.length} 个表格`);
    }

    // 调用Python脚本处理表格提取
    const { spawn } = require('child_process');

    // 使用确定的Python路径
    let pythonCommand = 'C:\\Users\\yoruh\\AppData\\Local\\Programs\\Python\\Python310\\python.exe';
    console.log(`使用Python路径: ${pythonCommand}`);

    // 直接使用配置中的脚本路径
    if (!scriptPath || !fs.existsSync(scriptPath)) {
      console.error('Python脚本不存在或路径无效:', scriptPath);
      return res.status(400).json({ success: false, message: "找不到Python脚本或路径无效" });
    }

    console.log(`使用脚本路径: ${scriptPath}`);
    console.log(`使用脚本: ${scriptPath}`);

    // 准备参考表格文件
    const refPath = path.join(__dirname, 'public', 'data', 'tables_reference.json');

    // 从当前表格数据创建参考文件
    if (fs.existsSync(tablesPath)) {
      try {
        const currentData = JSON.parse(fs.readFileSync(tablesPath, 'utf8'));
        // 使用所有表格作为参考，不仅限于特定页面
        console.log(`使用所有 ${currentData.tables.length} 个表格作为参考`);

        // 创建参考数据结构
        const referenceData = {
          metadata: { reference_for_page: page },
          tables: currentData.tables  // 使用所有表格
        };

        // 保存参考文件
        fs.writeFileSync(refPath, JSON.stringify(referenceData, null, 2));
        console.log(`已创建参考表格文件: ${refPath}`);
      } catch (err) {
        console.error('创建参考表格文件时出错:', err);
      }
    }

    console.log(`使用脚本路径: ${scriptPath}`);
    console.log(`使用脚本: ${scriptPath}`);

    const pythonArgs = [
      scriptPath,
      pdfPath,
      '--start', page.toString(),
      '--end', page.toString()
    ];

    // 如果存在参考文件，添加参考参数
    if (fs.existsSync(refPath)) {
      pythonArgs.push('--reference', refPath);
      console.log('已添加参考表格参数');
    }

    // 如果需要禁用跨页合并，添加相应参数
    if (exclude_merge) {
      pythonArgs.push('--exclude-merge');
    }

    console.log('执行命令:', pythonCommand, pythonArgs.join(' '));

    const pythonProcess = spawn(pythonCommand, pythonArgs);

    let dataString = '';
    let errorString = '';

    pythonProcess.stdout.on('data', (data) => {
      dataString += data.toString();
      console.log('Python输出:', data.toString());
    });

    pythonProcess.stderr.on('data', (data) => {
      errorString += data.toString();
      console.error('Python错误:', data.toString());
    });

    // 修改这部分代码
    pythonProcess.on('close', (code) => {
      console.log(`Python进程退出，代码: ${code}`);

      if (code !== 0) {
        return res.status(500).json({
          success: false,
          message: `Python处理失败 (代码: ${code})`,
          error: errorString
        });
      }

      // 直接处理表格合并，不使用setTimeout
      try {
        // 读取原始表格数据的备份
        if (!fs.existsSync(backupPath)) {
          console.log('没有找到备份文件，可能是首次运行');
          return res.json({ success: true, message: '表格已重新生成' });
        }

        // 直接检查Python脚本实际保存的位置
        const finalPath = path.join(__dirname, 'public', 'data', 'tables.json');

        if (!fs.existsSync(finalPath)) {
          console.error('无法找到生成的表格文件:', finalPath);
          return res.status(500).json({ success: false, message: '无法找到生成的表格文件' });
        }

        // 读取备份数据和新生成的数据
        const originalData = JSON.parse(fs.readFileSync(backupPath, 'utf8'));
        const newData = JSON.parse(fs.readFileSync(finalPath, 'utf8'));

        console.log(`备份中有 ${originalData.tables.length} 个表格`);
        console.log(`新生成的数据包含 ${newData.tables.length} 个表格`);

        // 如果新数据只有一个表格，表示它覆盖了原有数据
        if (newData.tables.length < originalData.tables.length) {
          console.log('检测到原有表格被覆盖，需要进行合并');

          // 从新数据中提取生成的表格
          const newTables = newData.tables;

          // 合并表格
          for (const newTable of newTables) {
            originalData.tables.push(newTable);
          }

          originalData.metadata.total_tables = originalData.tables.length;
          originalData.metadata.timestamp = new Date().toISOString();

          // 保存合并后的数据
          fs.writeFileSync(finalPath, JSON.stringify(originalData, null, 2));
          console.log(`成功合并表格，现在共有 ${originalData.tables.length} 个表格`);
        } else {
          console.log('新数据中的表格数量正常，无需特殊处理');
        }

        // 删除备份文件
        fs.unlinkSync(backupPath);
        console.log('已清理备份文件');

        return res.json({ success: true, message: '表格已重新生成并合并' });
      } catch (error) {
        console.error('处理表格数据时出错:', error);
        return res.status(500).json({
          success: false,
          message: '处理表格数据时出错',
          error: error.toString()
        });
      }
    });

  } catch (error) {
    console.error('重新生成表格失败:', error);
    res.status(500).json({ success: false, message: '重新生成表格失败', error: error.toString() });
  }
});
// 启动服务器
app.listen(PORT, () => {
  console.log(`服务器运行在 http://localhost:${PORT}`);
});
