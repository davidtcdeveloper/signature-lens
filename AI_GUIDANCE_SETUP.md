# AI Guidance System for SignatureLens

## Overview

This project uses a structured AI guidance system to ensure LLMs generate production-quality, spec-compliant code. The system is based on progressive disclosure: agents load only the rules they need for the current task.

## File Structure

```
SignatureLens/
├── AGENTS.md                    # Entry point for all AI agents
├── ai-rules/                    # Domain-specific rule files
│   ├── rule-loading.md         # Rule loading guide (load this FIRST)
│   ├── general.md              # Core Kotlin/Android patterns (ALWAYS load)
│   ├── camera.md               # Camera2 API patterns
│   ├── compose.md              # Jetpack Compose UI patterns
│   └── [future rules...]       # viewmodel.md, koin.md, etc.
└── specs/
    └── 0-early-spec.md         # Complete technical specification
```

## How It Works

### 1. AGENTS.md - The Entry Point

**Purpose:** High-level guide to the repository (architecture, commands, workflow)

**Contains:**
- Repository overview and philosophy
- Build/test commands
- Quick reference for common tasks
- Pointers to rule files and spec

**When to reference:** Always. This is the "README for AI agents."

### 2. ai-rules/rule-loading.md - The Smart Index

**Purpose:** Tells the LLM which rule files to load based on task context

**Contains:**
- Triggers and keywords for each rule file
- Quick reference scenarios
- Loading strategy and patterns
- Progressive disclosure guidelines

**When to reference:** ALWAYS load this first to understand which other rules you need.

### 3. Domain-Specific Rules

Each rule file covers one domain with:
- **Primary Directive:** Sets tone and non-negotiables
- **Cognitive Anchors:** Keywords that trigger this rule
- **Prioritized Rules:** HIGHEST, HIGH, MEDIUM priorities
- **Code Patterns:** Show don't tell (examples)
- **Checklists:** Concrete verification steps
- **Anti-Patterns:** Common mistakes to avoid

**Current Rules:**
- `general.md` - Core Android/Kotlin patterns (ALWAYS load)
- `camera.md` - Camera2 API, preview/capture pipelines
- `compose.md` - Jetpack Compose UI patterns

**Future Rules (to be created as needed):**
- `viewmodel.md` - State management patterns
- `koin.md` - Dependency injection
- `opengl.md` - Shader programming
- `encoding.md` - HEIC/MediaStore
- `testing.md` - Testing patterns
- `performance.md` - Optimization
- `commits.md` - Git workflow

### 4. specs/0-early-spec.md - The Source of Truth

**Purpose:** Complete technical specification defining WHAT to build

**The spec defines:**
- Product vision and success criteria
- Technical architecture
- Commands, structure, style
- Acceptance tests for each feature
- Known pitfalls and solutions
- Atomic task breakdown

**The rules define:**
- HOW to build it well
- Code patterns and best practices
- Common mistakes to avoid

## Usage Guide for AI Agents

### Step 1: Start with AGENTS.md
```
Read: @AGENTS.md
Purpose: Understand the project, commands, and where to find details
```

### Step 2: Load rule-loading.md
```
Read: @ai-rules/rule-loading.md
Purpose: Understand which rules to load for your current task
```

### Step 3: Load Relevant Rules
```
Based on task context, load:
- general.md (ALWAYS)
- 1-3 domain-specific rules

Example: For camera preview implementation
  → Load: general.md, camera.md, compose.md
```

### Step 4: Reference Spec Sections
```
Use spec as source of truth:
- Find task in §16 Roadmap
- Check acceptance criteria
- Review §14 pitfalls for domain
- Verify against §17 checklist
```

## Example Workflows

### Implementing Exposure Control
```
1. Load: @ai-rules/rule-loading.md (find relevant rules)
2. Load: @ai-rules/general.md (foundation)
3. Load: @ai-rules/camera.md (Camera2 API)
4. Load: @ai-rules/compose.md (UI component)
5. Reference: @specs/0-early-spec.md §2.1 (feature in scope)
6. Reference: @specs/0-early-spec.md §10.1 (ViewModel pattern)
7. Reference: @specs/0-early-spec.md §11.2 (PreviewScreen controls)
8. Implement following patterns from loaded rules
9. Verify: @specs/0-early-spec.md §17 checklist
```

### Writing Tests
```
1. Load: @ai-rules/rule-loading.md
2. Load: @ai-rules/general.md
3. Load: @ai-rules/testing.md (when created)
4. Reference: @specs/0-early-spec.md §12 (testing strategy)
5. Implement tests following patterns
6. Run: ./gradlew test connectedAndroidTest
```

### Code Review
```
1. Load: @ai-rules/rule-loading.md
2. Load: @ai-rules/general.md
3. Load: @ai-rules/commits.md (when created)
4. Reference: @specs/0-early-spec.md §8 (boundaries)
5. Check against loaded rules and spec
```

## Benefits

### For LLM Code Generation
- **Consistent Quality:** Code matches project patterns, not internet average
- **Fewer Iterations:** First pass closer to production-ready
- **Reduced Context:** Load only what's needed (no pollution)
- **Self-Verification:** Built-in checklists prevent common mistakes

### For Development Team
- **Onboarding:** New devs see standards explicitly
- **Documentation:** Patterns encoded with examples
- **Cross-Tool:** Works with Claude, ChatGPT, Cursor, Codex
- **Living Docs:** Rules evolve with project

## Rule File Format

Each rule file uses XML-like tags for structure:

```markdown
<primary_directive>
Core philosophy and non-negotiables
</primary_directive>

<cognitive_anchors>
Keywords that trigger this rule set
</cognitive_anchors>

<rule_1 priority="HIGHEST">
Rule description with code examples
</rule_1>

<pattern name="pattern_name">
Complete code pattern
</pattern>

<checklist>
Verification items
</checklist>

<avoid>
Anti-patterns and common mistakes
</avoid>
```

This format has proven reliable over 2+ years of production use.

## Progressive Disclosure Strategy

**Don't do this (context pollution):**
```
❌ Load all rule files for every task
❌ Dump entire spec into context window
❌ Re-explain patterns already in rules
```

**Do this (focused context):**
```
✅ Load 2-4 relevant rule files based on task
✅ Reference specific spec sections by number
✅ Refresh rules when switching task domains
✅ Keep context minimal and relevant
```

## Extending the System

### Adding New Rules

When you notice the LLM making the same mistake twice:

1. Create new rule file in `ai-rules/`
2. Define cognitive anchors (trigger keywords)
3. Add prioritized rules with examples
4. Include checklist and anti-patterns
5. Update `ai-rules/rule-loading.md` with triggers
6. Update `AGENTS.md` quick reference

### Updating Existing Rules

When patterns evolve:

1. Edit the rule file
2. Update examples to reflect new pattern
3. Add to checklist if verification needed
4. Document in anti-patterns if old way is now wrong

## Verification

Before claiming any task complete, agents must:

1. ✓ Load and follow relevant rules
2. ✓ Reference spec sections for requirements
3. ✓ Complete checklists from loaded rules
4. ✓ Run spec §17 self-verification
5. ✓ Pass tests and lint

If any step fails: STOP, fix, re-verify.

## Philosophy

> "LLMs give you the average of the internet. For production code, you need YOUR patterns, YOUR standards, YOUR best practices."

This system teaches LLMs to write SignatureLens code the way the SignatureLens team writes it—by encoding expertise explicitly.

---

**Remember:** Spec defines WHAT. Rules define HOW. Together they guide agents to production-quality code.
