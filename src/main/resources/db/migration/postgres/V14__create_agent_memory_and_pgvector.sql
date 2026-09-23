-- Migration V14: Enable pgvector extension and create agent_memories table for customer memory
CREATE EXTENSION IF NOT EXISTS vector;

-- Tier 2 Semantic & Episodic Memory table keyed by phone number
CREATE TABLE IF NOT EXISTS agent_memories (
    memory_id SERIAL PRIMARY KEY,
    phone VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50),
    memory_type VARCHAR(50) DEFAULT 'general', -- 'preference', 'order_history', 'note', 'prescription'
    content TEXT NOT NULL,
    embedding vector(768),
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_memories_phone ON agent_memories(phone);
CREATE INDEX IF NOT EXISTS idx_agent_memories_customer_id ON agent_memories(customer_id);
CREATE INDEX IF NOT EXISTS idx_agent_memories_type ON agent_memories(memory_type);

-- Ensure customer table has phone index for ultra-fast Tier 1 profile lookup
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
