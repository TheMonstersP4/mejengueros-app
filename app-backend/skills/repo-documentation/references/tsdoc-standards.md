# TSDoc Standards Reference

Use TSDoc to explain public contracts, non-obvious behavior, and business decisions.

Use:

```text
@remarks
@param
@returns
@throws
@example
@packageDocumentation
```

Avoid file headers on obvious files. Use `@packageDocumentation` for package or barrel entry points.

Document exported classes, use cases, ports, entities, value objects, and DTOs with business meaning. Avoid comments that repeat the code.
