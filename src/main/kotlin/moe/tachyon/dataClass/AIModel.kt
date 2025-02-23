@file:Suppress("PropertyName", "unused", "SpellCheckingInspection")

package moe.tachyon.dataClass

import kotlinx.serialization.Serializable

enum class AIModel(private val modelName: String) {
    DEEPSEEK_R1("deepseek-ai/DeepSeek-R1"),
    PRO_DEEPSEEK_R1("Pro/deepseek-ai/DeepSeek-R1"),
    DEEPSEEK_V3("deepseek-ai/DeepSeek-V3"),
    PRO_DEEPSEEK_V3("Pro/deepseek-ai/DeepSeek-V3"),
    DEEPSEEK_R1_DISTILL_LLAM_70B("deepseek-ai/DeepSeek-R1-Distill-Llama-70B"),
    DEEPSEEK_R1_DISTILL_QWEN_32B("deepseek-ai/DeepSeek-R1-Distill-Qwen-32B"),
    DEEPSEEK_R1_DISTILL_QWEN_14B("deepseek-ai/DeepSeek-R1-Distill-Qwen-14B"),
    DEEPSEEK_R1_DISTILL_LLAM_8B("deepseek-ai/DeepSeek-R1-Distill-Llama-8B"),
    DEEPSEEK_R1_DISTILL_QWEN_7B("deepseek-ai/DeepSeek-R1-Distill-Qwen-7B"),
    DEEPSEEK_R1_DISTILL_QWEN_1_5B("deepseek-ai/DeepSeek-R1-Distill-Qwen-1.5B"),
    PRO_DEEPSEEK_R1_DISTILL_LLAM_8B("Pro/deepseek-ai/DeepSeek-R1-Distill-Llama-8B"),
    PRO_DEEPSEEK_R1_DISTILL_QWEN_7B("Pro/deepseek-ai/DeepSeek-R1-Distill-Qwen-7B"),
    PRO_DEEPSEEK_R1_DISTILL_QWEN_1_5B("Pro/deepseek-ai/DeepSeek-R1-Distill-Qwen-1.5B"),
    META_LLAMA_LLAM_3_3_70B_INSTRUCT("meta-llama/Llama-3.3-70B-Instruct"),
    AIDC_AI_MARCO_O1("AIDC-AI/Marco-o1"),
    DEEPSEEK_V2_5("deepseek-ai/DeepSeek-V2.5"),
    QWEN_QWEN2_5_72B_INSTRUCT_128K("Qwen/Qwen2.5-72B-Instruct-128K"),
    QWEN_QWEN2_5_72B_INSTRUCT("Qwen/Qwen2.5-72B-Instruct"),
    QWEN_QWEN2_5_32B_INSTRUCT("Qwen/Qwen2.5-32B-Instruct"),
    QWEN_QWEN2_5_14B_INSTRUCT("Qwen/Qwen2.5-14B-Instruct"),
    QWEN_QWEN2_5_7B_INSTRUCT("Qwen/Qwen2.5-7B-Instruct"),
    QWEN_QWEN2_5_CODER_32B_INSTRUCT("Qwen/Qwen2.5-Coder-32B-Instruct"),
    QWEN_QWEN2_5_CODER_7B_INSTRUCT("Qwen/Qwen2.5-Coder-7B-Instruct"),
    QWEN_QWEN2_7B_INSTRUCT("Qwen/Qwen2-7B-Instruct"),
    QWEN_QWEN2_1_5B_INSTRUCT("Qwen/Qwen2-1.5B-Instruct"),
    QWEN_QWQ_32B_PREVIEW("Qwen/QwQ-32B-Preview"),
    TELEAI_TELECHAT2("TeleAI/TeleChat2"),
    AI_YI_1_5_34B_CHAT_16K("01-ai/Yi-1.5-34B-Chat-16K"),
    AI_YI_1_5_9B_CHAT_16K("01-ai/Yi-1.5-9B-Chat-16K"),
    AI_YI_1_5_6B_CHAT("01-ai/Yi-1.5-6B-Chat"),
    THUDM_GLM_4_9B_CHAT("THUDM/glm-4-9b-chat"),
    VENDOR_A_QWEN_QWEN2_5_72B_INSTRUCT("Vendor-A/Qwen/Qwen2.5-72B-Instruct"),
    INTERNLM_INTERNLM2_5_7B_CHAT("internlm/internlm2_5-7b-chat"),
    INTERNLM_INTERNLM2_5_20B_CHAT("internlm/internlm2_5-20b-chat"),
    NVIDIA_LLAMA_3_1_NEMOTRON_70B_INSTRUCT("nvidia/Llama-3.1-Nemotron-70B-Instruct"),
    META_LLAMA_META_LLAMA_3_1_405B_INSTRUCT("meta-llama/Meta-Llama-3.1-405B-Instruct"),
    META_LLAMA_META_LLAMA_3_1_70B_INSTRUCT("meta-llama/Meta-Llama-3.1-70B-Instruct"),
    META_LLAMA_META_LLAMA_3_1_8B_INSTRUCT("meta-llama/Meta-Llama-3.1-8B-Instruct"),
    GOOGLE_GEMMA_2_27B_IT("google/gemma-2-27b-it"),
    GOOGLE_GEMMA_2_9B_IT("google/gemma-2-9b-it"),
    PRO_QWEN_QWEN2_5_7B_INSTRUCT("Pro/Qwen/Qwen2.5-7B-Instruct"),
    PRO_QWEN_QWEN2_7B_INSTRUCT("Pro/Qwen/Qwen2-7B-Instruct"),
    PRO_QWEN_QWEN2_1_5B_INSTRUCT("Pro/Qwen/Qwen2-1.5B-Instruct"),
    PRO_THUDM_CHATGLM3_6B("Pro/THUDM/chatglm3-6b"),
    PRO_THUDM_GLM_4_9B_CHAT("Pro/THUDM/glm-4-9b-chat"),
    PRO_META_LLAMA_META_LLAMA_3_1_8B_INSTRUCT("Pro/meta-llama/Meta-Llama-3.1-8B-Instruct"),
    PRO_GOOGLE_GEMMA_2_9B_IT("Pro/google/gemma-2-9b-it");

    override fun toString(): String = modelName

    companion object{
        private fun fromString(value: String): AIModel? {
            return entries.find { it.modelName == value }
        }
        fun checkModelList(modelList: List<String>): Boolean {
            return modelList.all { modelName -> AIModel.fromString(modelName) != null }
        }
    }
}

@Serializable
data class DefaultModelConfig(
    val temperature: Double,
    val top_p: Double,
    val top_k: Int,
    val frequencyPenalty: Double,
    val modelList: List<AIModel>,
    val promptList: List<String>,
)
{
    companion object {
        val example get() = DefaultModelConfig(0.7, 0.95, 50, 0.0, emptyList(), emptyList())
    }
}
