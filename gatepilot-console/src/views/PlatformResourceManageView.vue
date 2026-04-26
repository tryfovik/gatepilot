<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>{{ activePage.label }}</h2>
          <p>{{ activePage.description }}</p>
        </div>
        <div class="panel-actions">
          <button class="ghost-button" type="button" :disabled="loading" @click="refresh">
            <RefreshCw :size="16" />
            刷新
          </button>
          <button class="ghost-button" type="button" @click="showRelatedPages = !showRelatedPages">
            {{ showRelatedPages ? '收起同组配置' : '同组配置' }}
          </button>
          <button class="primary-button" type="button" @click="startCreate">
            <Plus :size="16" />
            新增{{ activePage.label }}
          </button>
        </div>
      </div>

      <div v-if="showRelatedPages" class="compact-link-bar">
        <button
          v-for="page in relatedPages"
          :key="page.resourceType"
          class="compact-link-button"
          :class="{ 'compact-link-button--active': page.resourceType === activePage.resourceType }"
          type="button"
          @click="goPage(page.path)"
        >
          {{ page.label }}
        </button>
      </div>
    </section>

    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>{{ activePage.label }}列表</h2>
          <p>资源保存在 {{ platformNamespace }} 命名空间，项目接入和发布会引用这些配置</p>
        </div>
        <StatusBadge :label="items.length ? `${items.length} 条` : '暂无数据'" :tone="items.length ? 'info' : 'neutral'" />
      </div>
      <div class="filterbar">
        <input v-model="keyword" class="search-input" placeholder="搜索名称、团队、说明或参数键" />
      </div>

      <div v-if="error && !formDrawerOpen" class="state-box state-box--error state-box--compact">{{ error }}</div>
      <div v-if="loading" class="state-box">正在加载...</div>
      <div v-else-if="filteredItems.length === 0" class="empty-state">
        <div class="empty-state-title">暂无{{ activePage.label }}</div>
        <p>点击右上角新增{{ activePage.label }}，保存后接入向导和发布流程就可以直接选择。</p>
      </div>

      <table v-else class="resource-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>状态</th>
            <th>摘要</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in pagedItems" :key="item.metadata?.uid || item.metadata?.name">
            <td>
              <div class="resource-name">{{ item.metadata?.name || '-' }}</div>
              <div class="resource-subtitle">{{ item.spec?.displayName || item.spec?.key || '-' }}</div>
            </td>
            <td>
              <StatusBadge :label="statusLabel(item)" :tone="statusTone(item)" />
            </td>
            <td>{{ summaryText(item) }}</td>
            <td>{{ formatTime(item.metadata?.updatedAt || item.metadata?.createdAt) }}</td>
            <td>
              <div class="table-actions">
                <button class="table-action" type="button" @click="editItem(item)">编辑</button>
                <button class="table-action" type="button" @click="openItem(item)">查看</button>
                <button class="table-action table-action--danger" type="button" :disabled="saving" @click="deleteItem(item)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="filteredItems.length > pageSize" class="pagination-bar">
        <span>第 {{ currentPage }} / {{ pageCount }} 页，共 {{ filteredItems.length }} 条</span>
        <div class="table-actions">
          <button class="ghost-button" type="button" :disabled="currentPage <= 1" @click="currentPage -= 1">上一页</button>
          <button class="ghost-button" type="button" :disabled="currentPage >= pageCount" @click="currentPage += 1">下一页</button>
        </div>
      </div>
    </section>

    <div v-if="formDrawerOpen" class="drawer-backdrop form-drawer-backdrop" role="presentation" @click.self="closeFormDrawer">
      <form class="detail-drawer form-drawer" aria-label="资源编辑" @submit.prevent="saveCurrent">
        <div class="panel-header">
          <div>
            <h2>
              {{ editingName ? '编辑' : '新增' }}{{ activePage.label }}
            </h2>
            <p>{{ editingName ? '修改后保存，会更新控制面资源' : '保存后会出现在列表里，并可被项目接入引用' }}</p>
          </div>
          <button class="icon-button" type="button" aria-label="关闭" @click="closeFormDrawer">×</button>
        </div>

        <div class="drawer-content form-drawer-content">
          <div v-for="section in formSections" :key="section.key" class="form-subsection">
            <div class="form-section-title">
              <strong>{{ section.title }}</strong>
              <span>{{ section.note }}</span>
            </div>
            <div class="form-grid" :class="{ 'form-grid--optional': section.key === 'optional' }">
              <label v-for="field in section.fields" :key="field.key" :class="{ 'check-row': field.type === 'boolean' }">
                <template v-if="field.nameField">
                  <span>
                    {{ field.label }}
                    <em class="required-star" aria-label="必填">*</em>
                    <i class="help-dot" aria-hidden="true">i</i>
                  </span>
                  <input
                    v-model.trim="form.name"
                    class="search-input"
                    :disabled="Boolean(editingName)"
                    :placeholder="field.placeholder"
                    required
                  />
                </template>
                <template v-else-if="field.type === 'boolean'">
                  <input v-model="form[field.key]" type="checkbox" />
                  <span>
                    {{ field.label }}
                    <em v-if="field.required" class="required-star" aria-label="必填">*</em>
                    <i class="help-dot" aria-hidden="true">i</i>
                  </span>
                </template>
                <template v-else>
                  <span>
                    {{ field.label }}
                    <em v-if="field.required" class="required-star" aria-label="必填">*</em>
                    <i class="help-dot" aria-hidden="true">i</i>
                  </span>
                  <select v-if="field.options" v-model="form[field.key]" class="select-input" :required="field.required">
                    <option value="">请选择</option>
                    <option v-for="option in field.options" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                  <template v-else-if="field.relation">
                    <select
                      v-if="!field.relation.multiple"
                      v-model="form[field.key]"
                      class="select-input"
                      :disabled="relationMissing(field)"
                      :required="field.required"
                    >
                      <option value="">{{ field.relation.emptyLabel || `请选择${relationResourceLabel(field.relation.resourceType)}` }}</option>
                      <option v-for="option in relationOptions[field.relation.resourceType] || []" :key="option.value" :value="option.value">
                        {{ option.label }}
                      </option>
                    </select>
                    <div v-else-if="relationOptions[field.relation.resourceType]?.length" class="relation-chip-row">
                      <button
                        v-for="option in relationOptions[field.relation.resourceType]"
                        :key="option.value"
                        class="relation-chip"
                        :class="{ 'relation-chip--active': relationValueSelected(field.key, option.value) }"
                        type="button"
                        @click="toggleRelationValue(field.key, option.value)"
                      >
                        {{ option.label }}
                      </button>
                    </div>
                    <div v-else class="field-hint">
                      暂无可选{{ relationResourceLabel(field.relation.resourceType) }}，请先创建后再选择
                      <button class="inline-link-button" type="button" @click="goRelationPage(field.relation.resourceType)">去创建</button>
                    </div>
                  </template>
                  <textarea
                    v-else-if="field.type === 'textarea'"
                    v-model="form[field.key]"
                    class="text-input"
                    rows="3"
                    :required="field.required"
                  />
                  <input
                    v-else
                    v-model="form[field.key]"
                    class="search-input"
                    :type="field.type === 'number' ? 'number' : 'text'"
                    :required="field.required"
                  />
                </template>
                <small v-if="field.help" class="field-help-text">{{ field.help }}</small>
              </label>
            </div>
          </div>

          <div v-if="error" class="state-box state-box--error state-box--compact">{{ error }}</div>
          <div v-else-if="notice" class="state-box state-box--compact">{{ notice }}</div>
        </div>

        <div class="form-actions settings-actions form-drawer-actions">
          <button class="ghost-button" type="button" :disabled="saving" @click="closeFormDrawer">取消</button>
          <button class="primary-button" type="submit" :disabled="saving">
            <Save :size="16" />
            保存{{ activePage.label }}
          </button>
        </div>
      </form>
    </div>

    <ResourceDetailDrawer
      :open="Boolean(selectedItem)"
      :title="selectedItem?.metadata?.name || '资源详情'"
      :subtitle="activePage.label"
      :payload="selectedItem"
      @close="selectedItem = null"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Plus, RefreshCw, Save } from 'lucide-vue-next';
import ResourceDetailDrawer from '../components/ResourceDetailDrawer.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { deleteResource, listResources, saveResource } from '../api/client';
import {
  findPlatformResourcePage,
  FieldSchema,
  platformNamespace,
  platformResourcePages
} from '../config/platformResources';
import { StatusTone, formatTime, resourcePhaseLabel, resourcePhaseTone } from '../utils/format';
import { notifyError, notifySuccess, notifyWarning } from '../utils/feedback';

interface SettingsResource {
  metadata?: {
    uid?: string;
    name?: string;
    namespace?: string;
    createdAt?: string | number;
    updatedAt?: string | number;
  };
  spec?: Record<string, unknown> & {
    displayName?: string;
    description?: string;
    ownerTeam?: string;
    owner?: string;
    contact?: string;
    key?: string;
    host?: string;
    value?: string;
    defaultValue?: string;
    enabled?: boolean;
    acceptingProjects?: boolean;
    dedicated?: boolean;
    dedicatedSuggested?: boolean;
    hotReloadable?: boolean;
  };
  status?: {
    phase?: string;
    appliedValue?: string;
  };
}

const props = defineProps<{
  resourceType: string;
}>();

const router = useRouter();
const form = reactive<Record<string, any>>({});
const items = ref<SettingsResource[]>([]);
const keyword = ref('');
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const notice = ref('');
const editingName = ref('');
const selectedItem = ref<SettingsResource | null>(null);
const formDrawerOpen = ref(false);
const showRelatedPages = ref(false);
const relationOptions = ref<Record<string, RelationOption[]>>({});
const currentPage = ref(1);
const pageSize = 10;

interface RelationOption {
  label: string;
  value: string;
}

type ResourceFormField = FieldSchema & {
  nameField?: boolean;
  placeholder?: string;
};

interface ResourceFormSection {
  key: string;
  title: string;
  note: string;
  fields: ResourceFormField[];
}

const activePage = computed(() => findPlatformResourcePage(props.resourceType));
const relatedPages = computed(() =>
  platformResourcePages.filter((page) => page.group === activePage.value.group)
);
const formSections = computed<ResourceFormSection[]>(() => {
  const requiredFields: ResourceFormField[] = [
    {
      key: 'name',
      label: activePage.value.nameLabel,
      help: activePage.value.nameHelp,
      type: 'text',
      required: true,
      nameField: true,
      placeholder: activePage.value.namePlaceholder
    },
    ...activePage.value.fields.filter((field) => field.required)
  ];
  const optionalFields: ResourceFormField[] = activePage.value.fields.filter((field) => !field.required);
  return [
    {
      key: 'required',
      title: '基础信息',
      note: '先把资源身份和关键字段定下来',
      fields: requiredFields
    },
    {
      key: 'optional',
      title: '更多配置',
      note: '没有特殊要求时可以先使用默认值',
      fields: optionalFields
    }
  ].filter((section) => section.fields.length);
});

const filteredItems = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return items.value;
  }
  return items.value.filter((item) => [
    item.metadata?.name,
    item.spec?.displayName,
    item.spec?.ownerTeam,
    item.spec?.description,
    item.spec?.key,
    item.spec?.value
  ].filter(Boolean).join(' ').toLowerCase().includes(value));
});

const pageCount = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / pageSize)));
const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredItems.value.slice(start, start + pageSize);
});

onMounted(async () => {
  resetForm();
  await load();
});

watch(
  () => props.resourceType,
  async () => {
    resetForm();
    currentPage.value = 1;
    showRelatedPages.value = false;
    await load();
  }
);

watch(keyword, () => {
  currentPage.value = 1;
});

watch(pageCount, () => {
  if (currentPage.value > pageCount.value) {
    currentPage.value = pageCount.value;
  }
});

async function load(showToast = false) {
  loading.value = true;
  error.value = '';
  try {
    const [page] = await Promise.all([
      listResources<SettingsResource>(activePage.value.resourceType, platformNamespace),
      loadRelationOptions()
    ]);
    items.value = page.items;
    currentPage.value = 1;
    void showToast;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
    if (showToast) {
      notifyError(`${activePage.value.label}刷新失败`, error.value);
    }
  } finally {
    loading.value = false;
  }
}

async function loadRelationOptions() {
  const resourceTypes = Array.from(new Set(
    activePage.value.fields
      .map((field) => field.relation?.resourceType)
      .filter((value): value is string => Boolean(value))
  ));
  const entries = await Promise.all(resourceTypes.map(async (resourceType) => {
    try {
      const page = await listResources<SettingsResource>(resourceType, platformNamespace, 200);
      return [resourceType, page.items.map((item) => relationOption(resourceType, item)).filter((item) => item.value)] as const;
    } catch {
      return [resourceType, []] as const;
    }
  }));
  relationOptions.value = Object.fromEntries(entries);
}

function refresh() {
  void load(true);
}

function startCreate() {
  resetForm();
  formDrawerOpen.value = true;
}

function resetForm() {
  Object.keys(form).forEach((key) => delete form[key]);
  form.name = '';
  activePage.value.fields.forEach((field) => {
    form[field.key] = field.type === 'boolean' ? defaultBoolean(field.key) : '';
  });
  editingName.value = '';
  notice.value = '';
  error.value = '';
}

async function saveCurrent() {
  const validation = validateRequiredFields();
  if (!validation.passed) {
    error.value = validation.message;
    notifyWarning(error.value);
    return;
  }
  saving.value = true;
  error.value = '';
  notice.value = '';
  try {
    const resource = buildResource();
    await saveResource(activePage.value.resourceType, platformNamespace, String(form.name), resource);
    notice.value = `${activePage.value.label}已保存`;
    editingName.value = String(form.name);
    await load();
    formDrawerOpen.value = false;
    notifySuccess(`${activePage.value.label}已保存`, String(form.name));
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败';
    notifyError(`${activePage.value.label}保存失败`, error.value);
  } finally {
    saving.value = false;
  }
}

function editItem(item: SettingsResource) {
  resetForm();
  editingName.value = item.metadata?.name || '';
  form.name = editingName.value;
  activePage.value.fields.forEach((field) => {
    const value = item.spec?.[field.key];
    if (Array.isArray(value)) {
      form[field.key] = value.join(',');
    } else if (typeof value === 'boolean') {
      form[field.key] = value;
    } else {
      form[field.key] = value === undefined || value === null ? '' : String(value);
    }
  });
  formDrawerOpen.value = true;
}

function openItem(item: SettingsResource) {
  selectedItem.value = item;
}

function closeFormDrawer() {
  formDrawerOpen.value = false;
  resetForm();
}

async function deleteItem(item: SettingsResource) {
  const name = item.metadata?.name;
  if (!name) {
    return;
  }
  if (!window.confirm(`确认删除 ${activePage.value.label} ${name}？`)) {
    notifyWarning('已取消删除', name);
    return;
  }
  saving.value = true;
  try {
    await deleteResource(activePage.value.resourceType, platformNamespace, name);
    if (editingName.value === name) {
      resetForm();
    }
    await load();
    notifySuccess(`${activePage.value.label}已删除`, name);
  } catch (err) {
    notifyError(`${activePage.value.label}删除失败`, err instanceof Error ? err.message : '删除失败');
  } finally {
    saving.value = false;
  }
}

function buildResource(): SettingsResource {
  return {
    metadata: {
      name: String(form.name),
      namespace: platformNamespace
    },
    spec: buildSpec()
  };
}

function buildSpec() {
  const spec: Record<string, unknown> = {};
  activePage.value.fields.forEach((field) => {
    const value = form[field.key];
    if (field.relation && value && !validRelationValue(field, value)) {
      throw new Error(`${field.label}只能选择已创建的${relationResourceLabel(field.relation.resourceType)}`);
    }
    if (field.key === 'configShards') {
      spec[field.key] = String(value || '')
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);
      return;
    }
    if (field.type === 'number') {
      spec[field.key] = value === '' ? undefined : Number(value);
      return;
    }
    spec[field.key] = value;
  });
  return spec;
}

function validateRequiredFields() {
  const missingFields = [
    !String(form.name).trim() ? activePage.value.nameLabel : '',
    ...activePage.value.fields
      .filter((field) => field.required && isEmptyValue(form[field.key]))
      .map((field) => field.label)
  ].filter(Boolean);
  return {
    passed: missingFields.length === 0,
    message: missingFields.length ? `请先填写必填信息：${missingFields.join('、')}` : ''
  };
}

function isEmptyValue(value: unknown) {
  if (Array.isArray(value)) {
    return value.length === 0;
  }
  return value === undefined || value === null || String(value).trim() === '';
}

function defaultBoolean(key: string) {
  return !['dedicated', 'dedicatedSuggested'].includes(key);
}

function statusLabel(item: SettingsResource) {
  if (activePage.value.resourceType === 'control-plane-settings') {
    return item.spec?.hotReloadable === false ? '未启用热生效' : '可热生效';
  }
  return resourcePhaseLabel(item.status?.phase, '已配置');
}

function statusTone(item: SettingsResource): StatusTone {
  if (activePage.value.resourceType === 'control-plane-settings') {
    return item.spec?.hotReloadable === false ? 'warning' : 'success';
  }
  return item.status?.phase ? resourcePhaseTone(item.status.phase) : 'info';
}

function summaryText(item: SettingsResource) {
  const spec = item.spec || {};
  if (activePage.value.resourceType === 'namespaces') {
    return [spec.ownerTeam, spec.environment, `默认分片 ${spec.defaultConfigShard || '-'}`, `默认隔离组 ${spec.defaultIsolationGroup || '-'}`]
      .filter(Boolean)
      .join(' / ');
  }
  if (activePage.value.resourceType === 'teams') {
    return [spec.owner, spec.contact, spec.acceptingProjects === false ? '停止接入' : '允许接入']
      .filter(Boolean)
      .join(' / ');
  }
  if (activePage.value.resourceType === 'environments') {
    return [spec.tier, spec.acceptingProjects === false ? '停止接入' : '允许接入']
      .filter(Boolean)
      .join(' / ');
  }
  if (activePage.value.resourceType === 'config-shards') {
    return [spec.ownerTeam, `项目上限 ${spec.maxProjectCount || '-'}`, `路由上限 ${spec.maxRouteCount || '-'}`, spec.acceptingProjects === false ? '停止接入' : '允许接入']
      .filter(Boolean)
      .join(' / ');
  }
  if (activePage.value.resourceType === 'isolation-groups') {
    return [spec.ownerTeam, spec.dedicated ? '独享' : '共享', `RPS ${spec.maxRequestsPerSecond || '-'}`, `连接 ${spec.maxActiveConnections || '-'}`]
      .filter(Boolean)
      .join(' / ');
  }
  if (activePage.value.resourceType === 'traffic-tiers') {
    return [`RPS ${spec.maxRequestsPerSecond || '-'}`, `连接 ${spec.maxActiveConnections || '-'}`, `推荐分片 ${spec.recommendedConfigShard || '-'}`, `推荐隔离组 ${spec.recommendedIsolationGroup || '-'}`]
      .filter(Boolean)
      .join(' / ');
  }
  if (activePage.value.resourceType === 'ingress-domains') {
    return [spec.host, spec.ownerTeam, spec.defaultNamespace, spec.acceptingProjects === false ? '停止接入' : '允许接入']
      .filter(Boolean)
      .join(' / ');
  }
  return [`${spec.module || '-'} / ${spec.key || '-'}`, `当前值 ${spec.value || '-'}`, `默认 ${spec.defaultValue || '-'}`].join(' / ');
}

function relationOption(resourceType: string, item: SettingsResource): RelationOption {
  const value = String(item.metadata?.name || relationValue(resourceType, item) || '');
  const displayName = item.spec?.displayName ? String(item.spec.displayName) : '';
  const suffix = relationValue(resourceType, item);
  return {
    value,
    label: [displayName || value, suffix && suffix !== value ? suffix : ''].filter(Boolean).join(' / ')
  };
}

function relationValue(resourceType: string, item: SettingsResource) {
  if (resourceType === 'ingress-domains') {
    return item.spec?.host ? String(item.spec.host) : item.metadata?.name;
  }
  if (resourceType === 'control-plane-settings') {
    return item.spec?.key ? String(item.spec.key) : item.metadata?.name;
  }
  return item.metadata?.name;
}

function relationMissing(field: FieldSchema) {
  return Boolean(field.relation && !field.relation.multiple && !relationOptions.value[field.relation.resourceType]?.length);
}

function relationResourceLabel(resourceType: string) {
  return platformResourcePages.find((page) => page.resourceType === resourceType)?.label || '关联资源';
}

function relationValueSelected(key: string, value: string) {
  return splitRelationValues(form[key]).includes(value);
}

function toggleRelationValue(key: string, value: string) {
  const values = splitRelationValues(form[key]);
  const nextValues = values.includes(value)
    ? values.filter((item) => item !== value)
    : [...values, value];
  form[key] = nextValues.join(',');
}

function validRelationValue(field: FieldSchema, value: unknown) {
  if (!field.relation) {
    return true;
  }
  const allowed = new Set((relationOptions.value[field.relation.resourceType] || []).map((item) => item.value));
  return splitRelationValues(value).every((item) => allowed.has(item));
}

function splitRelationValues(value: unknown) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function goPage(path: string) {
  if (path === activePage.value.path) {
    return;
  }
  void router.push(path);
}

function goRelationPage(resourceType: string) {
  const page = platformResourcePages.find((item) => item.resourceType === resourceType);
  if (!page) {
    return;
  }
  formDrawerOpen.value = false;
  void router.push(page.path);
}
</script>
