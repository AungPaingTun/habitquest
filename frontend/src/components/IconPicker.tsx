import { useState } from 'react'
import { firstGrapheme, type IconGroup } from './iconChoices'

type Props = {
  groups: IconGroup[]
  value: string
  onChange: (icon: string) => void
  /** Tailwind color classes for the selected state, e.g. the habit (emerald) or prize (amber) accent. */
  accent: string
}

/** Grouped emoji grid plus a box to type or paste any other emoji. Clicking the selected icon clears it. */
export function IconPicker({ groups, value, onChange, accent }: Props) {
  const inGrid = groups.some((g) => g.icons.includes(value))
  const [custom, setCustom] = useState(value && !inGrid ? value : '')

  return (
    <fieldset>
      <legend className="text-sm text-slate-300 mb-2">Icon</legend>
      <div className="max-h-44 overflow-y-auto pr-1 space-y-2 rounded-lg border border-slate-800 p-2">
        {groups.map((group) => (
          <div key={group.name}>
            <p className="text-xs text-slate-500 mb-1">{group.name}</p>
            <div className="flex flex-wrap gap-1">
              {group.icons.map((emoji) => (
                <button
                  key={`${group.name}-${emoji}`}
                  type="button"
                  onClick={() => {
                    onChange(value === emoji ? '' : emoji)
                    setCustom('')
                  }}
                  aria-pressed={value === emoji}
                  aria-label={`Icon ${emoji}`}
                  className={`size-9 rounded-lg text-xl border transition-colors ${
                    value === emoji ? accent : 'border-transparent hover:border-slate-600 hover:bg-slate-800'
                  }`}
                >
                  {emoji}
                </button>
              ))}
            </div>
          </div>
        ))}
      </div>

      <label className="flex items-center gap-2 mt-2 text-sm text-slate-400">
        Or use any emoji:
        <input
          value={custom}
          onChange={(e) => {
            const emoji = firstGrapheme(e.target.value)
            setCustom(emoji)
            onChange(emoji)
          }}
          placeholder="🙂"
          aria-label="Custom emoji"
          className={`w-14 rounded-lg bg-slate-950 border px-2 py-1 text-center text-lg outline-none focus:ring-2 focus:ring-slate-500 ${
            custom ? accent : 'border-slate-700'
          }`}
        />
        <span className="text-xs text-slate-500">(Mac: Ctrl+Cmd+Space)</span>
      </label>
    </fieldset>
  )
}
