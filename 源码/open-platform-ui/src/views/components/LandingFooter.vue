<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-links">
        <div class="footer-section">
          <h5>关于我们</h5>
          <ul>
            <li><a href="https://www.nstl.gov.cn/Portal/zzjg_jgjj.html" target="_blank">组织机构</a></li>
            <li><a href="https://www.nstl.gov.cn/Portal/qgwftx_qgfwtxjs.html" target="_blank">全国服务体系</a></li>
            <li><a href="https://www.nstl.gov.cn/Portal/kxyj_kfhq.html" target="_blank">研究与发展</a></li>
            <li><a href="https://www.nstl.gov.cn/Portal/gjhz_scoap3.html" target="_blank">国际合作与交流</a></li>
            <li><a href="https://www.nstl.gov.cn/Portal/lxwm.html" target="_blank">联系我们</a></li>
            <li><a href="https://www.nstl.gov.cn/Portal/wxzyjs.html" target="_blank">文献资源建设</a></li>
          </ul>
        </div>
        <div class="footer-section">
          <h5>帮助</h5>
          <div class="footer-help-columns">
            <ul>
              <li><a href="https://www.nstl.gov.cn/Portal/zyyfw_zyjs.html" target="_blank">资源与服务</a></li>
              <li><a href="https://www.nstl.gov.cn/help/zzzx.html" target="_blank">个人中心</a></li>
              <li><a href="https://www.nstl.gov.cn/useradvise.html" target="_blank">意见反馈</a></li>
            </ul>
            <ul>
              <li><a href="https://anyask.nstl.gov.cn/ask" target="_blank">参考咨询</a></li>
              <li><a href="https://www.nstl.gov.cn/help/hotright.html" target="_blank">用户热线</a></li>
              <li><a href="https://www.nstl.gov.cn/help/copyright.html" target="_blank">版权声明</a></li>
            </ul>
          </div>
        </div>
      </div>

      <div class="footer-qr-codes">
        <div class="qr-item">
          <div class="qr-box">
            <img :src="officialWechatSrc" alt="官方微信" />
          </div>
          <span class="qr-label">官方微信</span>
        </div>
        <div class="qr-item">
          <div class="qr-box">
            <img :src="miniProgramSrc" alt="微信小程序" />
          </div>
          <span class="qr-label">微信小程序</span>
        </div>
      </div>

      <div class="footer-info">
        <div class="info-content">
          <div class="hotline">
            <span class="hotline-text">服务热线： 4008-161-200&nbsp;&nbsp;&nbsp;800-990-8900</span>
          </div>
          <div class="org-info">
            <div class="org-badge">
              <img :src="badgeSrc" alt="事业单位" />
            </div>
            <div class="org-details">
              <h2 class="org-name">国家科技图书文献中心</h2>
              <div class="contact-item">
                <span class="material-icons-outlined contact-icon">phone</span>
                <span>4008-161-200(免长途话费); 800-990-8900(免费); 010-58882057</span>
              </div>
              <div class="contact-item">
                <span class="material-icons-outlined contact-icon">email</span>
                <a href="mailto:service@nstl.gov.cn">service@nstl.gov.cn</a>
              </div>
            </div>
          </div>
          <div class="footer-bottom">
            <span class="address">地址：北京市复兴路15号</span>
            <div class="beian">
              <img :src="beianSrc" alt="" />
              <a href="https://beian.miit.gov.cn/#/Integrated/index" target="_blank">京ICP备10027328号</a>
            </div>
          </div>
          <div class="copyright">© Copyright(C)2023 NSTL All Rights Reserved 版权所有</div>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
const assetBase = import.meta.env.BASE_URL
const officialWechatSrc = `${assetBase}images/officialWeChat.png`
const miniProgramSrc = `${assetBase}images/miniProgram.png`
const badgeSrc = `${assetBase}images/blue.png`
const beianSrc = `${assetBase}images/beian.png`
</script>

<style lang="less" scoped>
.site-footer {
  background: #f2f8ff;
  border-top: 1px solid #dbeafe;
  padding: 48px 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 16px;
  line-height: normal;
  color: #303133;
  flex-shrink: 0;
}

.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 40px;
}

.footer-links {
  display: flex;
  gap: 48px;
}

.footer-section h5 {
  font-size: 16px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 20px;
}

.footer-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.footer-section ul li a {
  font-size: 14px;
  color: #2a7bdc;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-section ul li a:hover {
  color: #0f5fb0;
}

.footer-help-columns {
  display: flex;
  gap: 32px;
}

.footer-qr-codes {
  display: flex;
  gap: 24px;
  padding-top: 8px;
}

.qr-item {
  text-align: center;
}

.qr-box {
  background: #fff;
  padding: 4px;
  margin-bottom: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #f3f4f6;
}

.qr-box img {
  width: 96px;
  height: 96px;
  display: block;
  object-fit: contain;
}

.qr-label {
  font-size: 12px;
  color: #4b5563;
}

.footer-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.info-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.hotline {
  margin-bottom: 12px;
}

.hotline-text {
  color: #ff6e4a;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.org-info {
  display: flex;
  gap: 16px;
  text-align: left;
}

.org-badge {
  flex-shrink: 0;
  padding-top: 4px;
}

.org-badge img {
  width: 64px;
  height: auto;
  object-fit: contain;
}

.org-details {
  display: flex;
  flex-direction: column;
  max-width: 300px;
}

.org-name {
  font-size: 24px;
  font-weight: 700;
  color: #2b2b2b;
  letter-spacing: 0.5px;
  margin: 0 0 8px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #3a7bd5;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 6px;
}

.contact-item a {
  color: #3a7bd5;
  text-decoration: none;
}

.contact-item a:hover {
  text-decoration: underline;
}

.contact-icon {
  font-size: 18px;
}

.footer-bottom {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
  margin-bottom: 4px;
}

.address {
  color: #60656c;
}

.beian {
  display: flex;
  align-items: center;
  gap: 4px;
}

.beian img {
  width: 12px;
  height: 12px;
  object-fit: contain;
}

.beian a {
  color: #3a7bd5;
  text-decoration: none;
}

.beian a:hover {
  text-decoration: underline;
}

.copyright {
  font-size: 12px;
  color: #202020;
}

@media (max-width: 1024px) {
  .footer-inner {
    flex-direction: column;
    gap: 32px;
  }

  .footer-info {
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .site-footer {
    padding: 32px 16px;
    font-size: 14px;
  }

  .footer-links {
    flex-direction: column;
    gap: 24px;
  }

  .footer-help-columns {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
